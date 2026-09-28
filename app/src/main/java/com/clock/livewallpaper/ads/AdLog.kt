package com.clock.livewallpaper.ads

import android.util.Log
import com.clock.livewallpaper.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

/**
 * The ad layer's diagnostics channel: Logcat, tag `AdMobDebug`, debug builds only.
 *
 * Ad behaviour is deliberately invisible in the UI - there is no debug screen, no status row and
 * no test button anywhere in the app - so every question ("did the request go out?", "why is the
 * banner empty?") is answered by filtering Logcat for this tag. Release builds log nothing.
 *
 * Nothing user related is ever written here: only the ad format, the SDK's own error fields and
 * the response info Google returns for a failed request.
 */
internal object AdLog {

    const val TAG: String = "AdMobDebug"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun w(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }

    /** The four fields Google asks for when a load failure has to be reported. */
    fun loadFailed(format: String, error: LoadAdError) {
        if (!BuildConfig.DEBUG) return
        Log.w(
            TAG,
            "$format onAdFailedToLoad" +
                " code=${error.code}" +
                " domain=${error.domain}" +
                " message=${error.message}" +
                " cause=${error.cause}" +
                " responseInfo=${error.responseInfo}"
        )
    }

    /** A cached ad that could not be put on screen (expired, already shown, no Activity). */
    fun showFailed(format: String, error: AdError) {
        if (!BuildConfig.DEBUG) return
        Log.w(
            TAG,
            "$format onAdFailedToShowFullScreenContent" +
                " code=${error.code}" +
                " domain=${error.domain}" +
                " message=${error.message}"
        )
    }
}
