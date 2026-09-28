package com.google.android.gms.ads.nativead

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.gms.ads.ResponseInfo

interface MediaContent {
    val aspectRatio: Float
    val hasVideoContent: Boolean
    val duration: Float
}

abstract class NativeAd {
    abstract val headline: String?
    abstract val body: String?
    abstract val callToAction: String?
    abstract val advertiser: String?
    abstract val store: String?
    abstract val price: String?
    abstract val icon: Image?
    abstract val images: List<Image>
    abstract val mediaContent: MediaContent?
    abstract val starRating: Double?
    abstract val responseInfo: ResponseInfo?
    abstract fun destroy()

    abstract class Image {
        abstract val drawable: Drawable?
        abstract val uri: Uri?
        abstract val scale: Double
    }

    fun interface OnNativeAdLoadedListener {
        fun onNativeAdLoaded(nativeAd: NativeAd)
    }
}

class MediaView(context: Context) : FrameLayout(context) {
    var mediaContent: MediaContent? = null
    fun setImageScaleType(scaleType: ImageView.ScaleType) {}
}

class NativeAdView(context: Context) : FrameLayout(context) {
    var headlineView: View? = null
    var bodyView: View? = null
    var callToActionView: View? = null
    var iconView: View? = null
    var advertiserView: View? = null
    var priceView: View? = null
    var starRatingView: View? = null
    var storeView: View? = null
    var mediaView: MediaView? = null
    fun setNativeAd(ad: NativeAd) {}
    fun destroy() {}
}

class NativeAdOptions private constructor() {
    class Builder {
        fun setAdChoicesPlacement(placement: Int): Builder = this
        fun setRequestMultipleImages(request: Boolean): Builder = this
        fun build(): NativeAdOptions = NativeAdOptions()
    }

    companion object {
        const val ADCHOICES_TOP_LEFT = 0
        const val ADCHOICES_TOP_RIGHT = 1
        const val ADCHOICES_BOTTOM_RIGHT = 2
        const val ADCHOICES_BOTTOM_LEFT = 3
    }
}
