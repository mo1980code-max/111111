package com.clock.livewallpaper.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app-open format: cache one creative, use it at most once when the user really comes back.
 *
 * This manager knows nothing about lifecycles on purpose. The single Activity reports
 * `onStart` / `onStop` to [AdsCoordinator], which owns the rules ("was the app away long enough?",
 * "is a permission or consent screen involved?", "was another full-screen ad just shown?") and
 * calls [show] only when all of them pass. That keeps every foreground rule in one file and keeps
 * an Activity reference out of every singleton.
 */
@Singleton
class AppOpenAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val loading = AtomicBoolean(false)
    private var ad: AppOpenAd? = null
    private var loadedAtMs: Long = 0L
    private var showing: Boolean = false

    val isShowing: Boolean
        get() = showing

    @MainThread
    fun preload() {
        if (cached() != null || showing) return
        if (!loading.compareAndSet(false, true)) return

        AdLog.d("app open ad request started unit=${AdConfig.APP_OPEN_UNIT_ID}")
        AppOpenAd.load(
            context,
            AdConfig.APP_OPEN_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    AdLog.d("app open onAdLoaded response=${loaded.responseInfo}")
                    ad = loaded
                    loadedAtMs = SystemClock.elapsedRealtime()
                    loading.set(false)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLog.loadFailed("app open", error)
                    ad = null
                    loading.set(false)
                }
            }
        )
    }

    /** @return true when the cached creative was handed to the SDK. Never blocks the app. */
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
                AdLog.d("app open onAdShowedFullScreenContent")
            }

            override fun onAdImpression() {
                AdLog.d("app open onAdImpression")
            }

            override fun onAdClicked() {
                AdLog.d("app open onAdClicked")
            }

            override fun onAdDismissedFullScreenContent() {
                AdLog.d("app open onAdDismissedFullScreenContent")
                showing = false
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdLog.showFailed("app open", error)
                showing = false
                preload()
            }
        }

        ad = null
        showing = true
        ready.show(activity)
        return true
    }

    private fun cached(): AppOpenAd? {
        val current = ad ?: return null
        if (SystemClock.elapsedRealtime() - loadedAtMs > AdConfig.APP_OPEN_MAX_AGE_MS) {
            AdLog.d("app open cache expired, dropping it")
            ad = null
            return null
        }
        return current
    }
}
