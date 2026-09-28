// The full screen formats of play-services-ads 25.4.0, transcribed from the public reference.
package com.google.android.gms.ads.interstitial

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenAd

abstract class InterstitialAd : FullScreenAd() {
    val adUnitId: String = ""

    companion object {
        fun load(
            context: Context,
            adUnitId: String,
            adRequest: AdRequest,
            callback: InterstitialAdLoadCallback
        ) {
        }
    }
}

abstract class InterstitialAdLoadCallback : AdLoadCallback<InterstitialAd>()
