package com.clock.livewallpaper.ads

import android.app.Activity
import android.os.SystemClock
import androidx.annotation.MainThread
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place that decides whether an advert may be requested, and whether a full-screen one may
 * appear right now.
 *
 * Everything the product promised about advertising is expressed here instead of being spread over
 * the screens:
 *
 * * **no ad before it is allowed** - [adsReady] is `canRequestAds` (UMP) *and* an initialised SDK
 *   *and* finished onboarding; banner, native and every preload wait for it;
 * * **consent first** - the SDK is only initialised once UMP allows ad requests, and the consent
 *   flow itself is only started after onboarding, so a first-run user meets the app, not a form;
 * * **full-screen ads are rare** - a warm-up after launch, a minimum distance between two of them,
 *   nothing while a consent form or system UI is or was just on screen, nothing during onboarding;
 * * **no Activity is kept** - Activities arrive as parameters of [gatherConsent], [onForeground],
 *   [onContentSessionEnded] and [showPrivacyOptions] and are used immediately.
 */
@Singleton
class AdsCoordinator @Inject constructor(
    private val consentManager: ConsentManager,
    private val initializer: MobileAdsInitializer,
    private val interstitialAds: InterstitialAdManager,
    private val appOpenAds: AppOpenAdManager,
    settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val onboardingCompleted: StateFlow<Boolean> = settings.onboardingCompleted
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** Ads may be requested: consent allows it, the SDK is up and onboarding is behind us. */
    val adsReady: StateFlow<Boolean> = combine(
        consentManager.state,
        initializer.initialized,
        onboardingCompleted
    ) { consent, initialized, onboarded ->
        consent.canRequestAds && initialized && onboarded
    }.stateIn(scope, SharingStarted.Eagerly, false)

    /** UMP asks for a privacy options entry point; settings shows its row only then. */
    val privacyOptionsRequired: StateFlow<Boolean> = consentManager.state
        .map { it.privacyOptionsRequired }
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** The UMP flow still has to run for this launch - the Activity watches this and calls in. */
    val consentPending: StateFlow<Boolean> = combine(
        consentManager.state,
        onboardingCompleted
    ) { consent, onboarded ->
        onboarded && !consent.gathered
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private var foregroundCount: Int = 0
    private var sessionStartedAtMs: Long = 0L
    private var backgroundSinceMs: Long = 0L
    private var quietUntilMs: Long = 0L
    private var lastFullscreenAtMs: Long = 0L
    private var finishedContentSessions: Int = 0

    init {
        // Consent decides initialisation; initialisation plus consent decide the preloads. Both
        // collectors live on the main thread because the Mobile Ads SDK loads ads there.
        scope.launch(Dispatchers.Main.immediate) {
            consentManager.state.collect { consent ->
                if (consent.canRequestAds) initializer.ensureInitialized()
            }
        }
        scope.launch(Dispatchers.Main.immediate) {
            adsReady.collect { ready ->
                if (!ready) return@collect
                AdLog.d("ads are allowed: consent granted, SDK initialised, onboarding done")
                interstitialAds.preload()
                appOpenAds.preload()
            }
        }
    }

    // ------------------------------------------------------------------ consent

    /** Refreshes UMP consent with a foreground Activity. Repeated calls are no-ops. */
    @MainThread
    fun gatherConsent(activity: Activity) {
        markQuietPeriod()
        consentManager.gather(activity)
    }

    /** The settings entry point for Google's privacy options form. */
    @MainThread
    fun showPrivacyOptions(activity: Activity) {
        markQuietPeriod()
        consentManager.showPrivacyOptions(activity)
    }

    // --------------------------------------------------------------- lifecycle

    /**
     * The single Activity came to the front.
     *
     * The first foreground of the process is the cold start: it never carries an app-open ad. Any
     * later one does, but only after a real absence, and only if the full-screen budget allows it.
     */
    @MainThread
    fun onForeground(activity: Activity) {
        val now = SystemClock.elapsedRealtime()
        val awayFrom = backgroundSinceMs
        backgroundSinceMs = 0L
        foregroundCount += 1

        if (foregroundCount == 1) {
            sessionStartedAtMs = now
            AdLog.d("cold start: app open ad deliberately skipped")
            return
        }

        val awayMs = if (awayFrom == 0L) 0L else now - awayFrom
        if (awayMs < AdConfig.APP_OPEN_MIN_BACKGROUND_MS) {
            AdLog.d("app open skipped: only ${awayMs}ms in the background")
            return
        }
        if (!canShowFullscreen("app open")) return
        if (appOpenAds.show(activity)) markFullscreenShown()
    }

    /** The single Activity left the front; the clock for "was the user really away?" starts. */
    @MainThread
    fun onBackground() {
        backgroundSinceMs = SystemClock.elapsedRealtime()
    }

    /**
     * The app handed the screen to something else - a runtime permission prompt, the overlay
     * permission screen, a share sheet. The Activity reports it centrally, and no full-screen ad
     * runs while that is happening or in the moments after the user comes back.
     */
    @MainThread
    fun onSystemUiShown() {
        markQuietPeriod()
    }

    // ----------------------------------------------------------------- content

    /**
     * A guided reading session just ended - the natural break of this app, and the only moment an
     * interstitial is even considered. The first session is always free of ads.
     */
    @MainThread
    fun onContentSessionEnded(activity: Activity) {
        finishedContentSessions += 1
        if (finishedContentSessions < AdConfig.INTERSTITIAL_MIN_SESSIONS) {
            AdLog.d("interstitial skipped: reading session $finishedContentSessions of this launch")
            return
        }
        if (!canShowFullscreen("interstitial")) return
        if (interstitialAds.show(activity)) markFullscreenShown()
    }

    // -------------------------------------------------------------------- gate

    private fun canShowFullscreen(format: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (!adsReady.value) {
            AdLog.d("$format skipped: ads are not allowed yet")
            return false
        }
        if (consentManager.formVisible.value || appOpenAds.isShowing) {
            AdLog.d("$format skipped: another form or ad owns the screen")
            return false
        }
        if (now < quietUntilMs) {
            AdLog.d("$format skipped: quiet window after consent, permission or system UI")
            return false
        }
        if (now - sessionStartedAtMs < AdConfig.SESSION_WARM_UP_MS) {
            AdLog.d("$format skipped: the session is still warming up")
            return false
        }
        if (lastFullscreenAtMs != 0L && now - lastFullscreenAtMs < AdConfig.FULLSCREEN_MIN_GAP_MS) {
            AdLog.d("$format skipped: the previous full-screen ad is too recent")
            return false
        }
        return true
    }

    private fun markFullscreenShown() {
        lastFullscreenAtMs = SystemClock.elapsedRealtime()
        markQuietPeriod()
    }

    private fun markQuietPeriod() {
        quietUntilMs = SystemClock.elapsedRealtime() + AdConfig.SENSITIVE_FLOW_QUIET_MS
    }
}
