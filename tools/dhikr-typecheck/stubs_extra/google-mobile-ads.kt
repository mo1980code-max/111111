// Declarations of the Google Mobile Ads API surface this app uses, transcribed from the public
// reference of play-services-ads 25.4.0. No Android SDK and no Google Maven are reachable from
// this workspace, so the type-check compiles against these instead of the real artifact: every
// name, parameter list and nullability below mirrors the documented API.
package com.google.android.gms.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

class ResponseInfo {
    val responseId: String? = null
    val mediationAdapterClassName: String? = null
}

open class AdError(val code: Int, val message: String, val domain: String) {
    val cause: AdError? = null
}

class LoadAdError(code: Int, message: String, domain: String) : AdError(code, message, domain) {
    val responseInfo: ResponseInfo? = null
}

class AdRequest private constructor() {
    class Builder {
        fun build(): AdRequest = AdRequest()
    }
}

class AdSize(val width: Int, val height: Int) {
    companion object {
        val BANNER: AdSize = AdSize(320, 50)
        val FULL_BANNER: AdSize = AdSize(468, 60)

        fun getCurrentOrientationAnchoredAdaptiveBannerAdSize(context: Context, width: Int): AdSize =
            AdSize(width, 50)

        fun getLargeAnchoredAdaptiveBannerAdSize(context: Context, width: Int): AdSize =
            AdSize(width, 100)
    }
}

open class AdListener {
    open fun onAdLoaded() {}
    open fun onAdFailedToLoad(error: LoadAdError) {}
    open fun onAdImpression() {}
    open fun onAdClicked() {}
    open fun onAdOpened() {}
    open fun onAdClosed() {}
}

open class FullScreenContentCallback {
    open fun onAdShowedFullScreenContent() {}
    open fun onAdDismissedFullScreenContent() {}
    open fun onAdFailedToShowFullScreenContent(error: AdError) {}
    open fun onAdImpression() {}
    open fun onAdClicked() {}
}

open class BaseAdView(context: Context) : ViewGroup(context) {
    var adUnitId: String? = null
    var adListener: AdListener = AdListener()
    val adSize: AdSize? = null
    val responseInfo: ResponseInfo? = null
    fun setAdSize(size: AdSize) {}
    fun loadAd(request: AdRequest) {}
    fun pause() {}
    fun resume() {}
    fun destroy() {}
}

class AdView(context: Context) : BaseAdView(context)

class AdapterStatus {
    enum class State { NOT_READY, READY }

    val initializationState: State = State.READY
    val description: String = ""
    val latency: Int = 0
}

interface InitializationStatus {
    val adapterStatusMap: Map<String, AdapterStatus>
}

fun interface OnInitializationCompleteListener {
    fun onInitializationComplete(status: InitializationStatus)
}

fun interface OnUserEarnedRewardListener {
    fun onUserEarnedReward(rewardItem: com.google.android.gms.ads.rewarded.RewardItem)
}

object MobileAds {
    fun initialize(context: Context) {}
    fun initialize(context: Context, listener: OnInitializationCompleteListener) {}
    fun setAppMuted(muted: Boolean) {}
}

class AdLoader private constructor() {
    fun loadAd(request: AdRequest) {}
    fun loadAds(request: AdRequest, count: Int) {}
    val isLoading: Boolean = false

    class Builder(context: Context, adUnitId: String) {
        fun forNativeAd(listener: NativeAd.OnNativeAdLoadedListener): Builder = this
        fun withAdListener(listener: AdListener): Builder = this
        fun withNativeAdOptions(options: NativeAdOptions): Builder = this
        fun build(): AdLoader = AdLoader()
    }
}

/** The base every format specific load callback extends; the full screen formats subclass it. */
abstract class AdLoadCallback<T> {
    open fun onAdLoaded(ad: T) {}
    open fun onAdFailedToLoad(error: LoadAdError) {}
}

/** Shared surface of the full screen formats: one callback, one Activity aware show call. */
abstract class FullScreenAd {
    var fullScreenContentCallback: FullScreenContentCallback? = null
    val responseInfo: ResponseInfo? = null
    abstract fun show(activity: Activity)
}
