package com.clock.livewallpaper.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preload, show once, reload - and nothing else.
 *
 * The manager owns the cache and the SDK callbacks; **when** an interstitial may appear is not its
 * decision but [AdsCoordinator]'s, which is why [show] can be called freely: it refuses whenever
 * there is nothing valid to display. The cached ad is loaded with the application context and only
 * the `show` call receives an Activity, so no screen can leak into this singleton.
 */
@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val loading = AtomicBoolean(false)
    private var ad: InterstitialAd? = null
    private var loadedAtMs: Long = 0L
    private var showing: Boolean = false

    /** Requests the next interstitial unless one is already cached or on its way. */
    @MainThread
    fun preload() {
        if (cached() != null || showing) return
        if (!loading.compareAndSet(false, true)) return

        AdLog.d("interstitial ad request started unit=${AdConfig.INTERSTITIAL_UNIT_ID}")
        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    AdLog.d("interstitial onAdLoaded response=${loaded.responseInfo}")
                    ad = loaded
                    loadedAtMs = SystemClock.elapsedRealtime()
                    loading.set(false)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLog.loadFailed("interstitial", error)
                    ad = null
                    loading.set(false)
                }
            }
        )
    }

    /**
     * Puts a cached interstitial on screen.
     *
     * @return true when the ad was handed to the SDK, false when there was nothing to show - in
     * which case the next one is requested so the following opportunity is covered.
     */
    @MainThread
    fun show(activity: Activity): Boolean {
        if (showing) return false
        if (activity.isFinishing || activity.isDestroyed) return false
        val ready = cached()
        if (ready == null) {
            preload()
            return false
        }

        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdLog.d("interstitial onAdShowedFullScreenContent")
            }

            override fun onAdImpression() {
                AdLog.d("interstitial onAdImpression")
            }

            override fun onAdClicked() {
                AdLog.d("interstitial onAdClicked")
            }

            override fun onAdDismissedFullScreenContent() {
                AdLog.d("interstitial onAdDismissedFullScreenContent")
                showing = false
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdLog.showFailed("interstitial", error)
                showing = false
                preload()
            }
        }

        // One creative can only be shown once: drop it before the SDK takes over.
        ad = null
        showing = true
        ready.show(activity)
        return true
    }

    private fun cached(): InterstitialAd? {
        val current = ad ?: return null
        if (SystemClock.elapsedRealtime() - loadedAtMs > AdConfig.CACHED_AD_MAX_AGE_MS) {
            AdLog.d("interstitial cache expired, dropping it")
            ad = null
            return null
        }
        return current
    }
}
