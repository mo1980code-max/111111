package com.clock.livewallpaper.ads

import android.content.Context
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handle for one native ad request, owned by the slot that asked for it.
 *
 * A native ad is a native *object*: it holds bitmaps and a video controller and has to be
 * destroyed by hand. The screen can go away while a request is still in the air, so its owner
 * cancels the handle and the manager destroys the late arrival instead of handing it to a screen
 * that no longer exists.
 */
class NativeAdRequest internal constructor() {

    @Volatile
    internal var isCancelled: Boolean = false
        private set

    fun cancel() {
        isCancelled = true
    }
}

/**
 * Native Advanced ads.
 *
 * The manager never caches an ad: one slot, one request, one owner. Whoever receives the ad in
 * [load] is responsible for destroying it when it is replaced or when its UI disappears - which is
 * exactly what [AdsViewModel] does for the one native card of a screen.
 */
@Singleton
class NativeAdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Starts one native ad request.
     *
     * @param onLoaded called on the main thread with an ad the caller now owns.
     * @return the handle the caller must cancel when its slot leaves the screen.
     */
    @MainThread
    fun load(onLoaded: (NativeAd) -> Unit): NativeAdRequest {
        val request = NativeAdRequest()
        val options = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .build()

        AdLog.d("native ad request started unit=${AdConfig.NATIVE_UNIT_ID}")
        val loader = AdLoader.Builder(context, AdConfig.NATIVE_UNIT_ID)
            .forNativeAd { nativeAd ->
                if (request.isCancelled) {
                    AdLog.d("native ad arrived after its slot was disposed, destroying it")
                    nativeAd.destroy()
                } else {
                    AdLog.d("native onAdLoaded response=${nativeAd.responseInfo}")
                    onLoaded(nativeAd)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLog.loadFailed("native", error)
                }

                override fun onAdImpression() {
                    AdLog.d("native onAdImpression")
                }

                override fun onAdClicked() {
                    AdLog.d("native onAdClicked")
                }
            })
            .withNativeAdOptions(options)
            .build()

        loader.loadAd(AdRequest.Builder().build())
        return request
    }
}
