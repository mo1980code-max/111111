package com.google.android.gms.ads.appopen

import android.content.Context
import com.google.android.gms.ads.AdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenAd

abstract class AppOpenAd : FullScreenAd() {
    val adUnitId: String = ""

    abstract class AppOpenAdLoadCallback : AdLoadCallback<AppOpenAd>()

    companion object {
        fun load(
            context: Context,
            adUnitId: String,
            adRequest: AdRequest,
            callback: AppOpenAdLoadCallback
        ) {
        }
    }
}
