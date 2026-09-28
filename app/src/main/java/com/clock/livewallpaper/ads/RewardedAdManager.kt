package com.clock.livewallpaper.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewarded ads, ready for the day a reward means something here.
 *
 * The app deliberately has **no placement** for this format: an adhkar counter, a tasbeeh round or
 * a floating card are not features to be unlocked by watching an advert, and inventing one just to
 * spend the format would damage the product. So the manager exists, is fully wired and is never
 * called by [AdsCoordinator]: nothing in the app can start a rewarded ad on its own.
 *
 * When a placement does arrive, the contract is already fixed here: [load] is what a screen calls
 * while the user is deciding, [show] may only run from an explicit user action, and the reward is
 * handed over exclusively from Google's earned-reward callback - never from a dismiss listener.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val loading = AtomicBoolean(false)
    private var ad: RewardedAd? = null
    private var loadedAtMs: Long = 0L
    private var showing: Boolean = false

    val isReady: Boolean
        get() = cached() != null

    @MainThread
    fun load() {
        if (cached() != null || showing) return
        if (!loading.compareAndSet(false, true)) return

        AdLog.d("rewarded ad request started unit=${AdConfig.REWARDED_UNIT_ID}")
        RewardedAd.load(
            context,
            AdConfig.REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    AdLog.d("rewarded onAdLoaded response=${loaded.responseInfo}")
                    ad = loaded
                    loadedAtMs = SystemClock.elapsedRealtime()
                    loading.set(false)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLog.loadFailed("rewarded", error)
                    ad = null
                    loading.set(false)
                }
            }
        )
    }

    /**
     * Shows the cached rewarded ad. Must only be called from a user action that asked for it.
     *
     * @param onRewardEarned invoked once, from the SDK's earned-reward callback and nowhere else.
     */
    @MainThread
    fun show(activity: Activity, onRewardEarned: (RewardItem) -> Unit): Boolean {
        if (showing) return false
        if (activity.isFinishing || activity.isDestroyed) return false
        val ready = cached()
        if (ready == null) {
            load()
            return false
        }

        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdLog.d("rewarded onAdShowedFullScreenContent")
            }

            override fun onAdImpression() {
                AdLog.d("rewarded onAdImpression")
            }

            override fun onAdClicked() {
                AdLog.d("rewarded onAdClicked")
            }

            override fun onAdDismissedFullScreenContent() {
                AdLog.d("rewarded onAdDismissedFullScreenContent")
                showing = false
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdLog.showFailed("rewarded", error)
                showing = false
            }
        }

        ad = null
        showing = true
        ready.show(activity) { reward ->
            AdLog.d("rewarded onUserEarnedReward type=${reward.type} amount=${reward.amount}")
            onRewardEarned(reward)
        }
        return true
    }

    private fun cached(): RewardedAd? {
        val current = ad ?: return null
        if (SystemClock.elapsedRealtime() - loadedAtMs > AdConfig.CACHED_AD_MAX_AGE_MS) {
            AdLog.d("rewarded cache expired, dropping it")
            ad = null
            return null
        }
        return current
    }
}
