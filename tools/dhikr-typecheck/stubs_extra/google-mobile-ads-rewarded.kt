package com.google.android.gms.ads.rewarded

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenAd
import com.google.android.gms.ads.OnUserEarnedRewardListener

interface RewardItem {
    val type: String
    val amount: Int
}

abstract class RewardedAd : FullScreenAd() {
    val adUnitId: String = ""

    abstract fun show(activity: Activity, listener: OnUserEarnedRewardListener)

    companion object {
        fun load(
            context: Context,
            adUnitId: String,
            adRequest: AdRequest,
            callback: RewardedAdLoadCallback
        ) {
        }
    }
}

abstract class RewardedAdLoadCallback : AdLoadCallback<RewardedAd>()
