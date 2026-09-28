// Google's User Messaging Platform (user-messaging-platform 4.0.0), transcribed from the public
// reference: https://developers.google.com/admob/android/privacy
package com.google.android.ump

import android.app.Activity
import android.content.Context

class FormError(val errorCode: Int, val message: String)

interface ConsentForm {
    fun show(activity: Activity, listener: OnConsentFormDismissedListener)

    fun interface OnConsentFormDismissedListener {
        fun onConsentFormDismissed(formError: FormError?)
    }
}

class ConsentRequestParameters private constructor() {
    class Builder {
        fun setTagForUnderAgeOfConsent(underAge: Boolean): Builder = this
        fun setConsentDebugSettings(settings: ConsentDebugSettings): Builder = this
        fun build(): ConsentRequestParameters = ConsentRequestParameters()
    }
}

class ConsentDebugSettings private constructor() {
    class Builder(context: Context) {
        fun setDebugGeography(geography: Int): Builder = this
        fun addTestDeviceHashedId(hashedId: String): Builder = this
        fun build(): ConsentDebugSettings = ConsentDebugSettings()
    }
}

interface ConsentInformation {
    val consentStatus: Int
    val privacyOptionsRequirementStatus: PrivacyOptionsRequirementStatus
    fun canRequestAds(): Boolean
    fun isConsentFormAvailable(): Boolean
    fun reset()
    fun requestConsentInfoUpdate(
        activity: Activity,
        params: ConsentRequestParameters,
        successListener: OnConsentInfoUpdateSuccessListener,
        failureListener: OnConsentInfoUpdateFailureListener
    )

    enum class PrivacyOptionsRequirementStatus { UNKNOWN, NOT_REQUIRED, REQUIRED }

    object ConsentStatus {
        const val UNKNOWN = 0
        const val NOT_REQUIRED = 1
        const val REQUIRED = 2
        const val OBTAINED = 3
    }

    fun interface OnConsentInfoUpdateSuccessListener {
        fun onConsentInfoUpdateSuccess()
    }

    fun interface OnConsentInfoUpdateFailureListener {
        fun onConsentInfoUpdateFailure(error: FormError)
    }
}

object UserMessagingPlatform {
    fun getConsentInformation(context: Context): ConsentInformation = StubConsentInformation

    fun loadAndShowConsentFormIfRequired(
        activity: Activity,
        listener: ConsentForm.OnConsentFormDismissedListener
    ) {
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        listener: ConsentForm.OnConsentFormDismissedListener
    ) {
    }
}

private object StubConsentInformation : ConsentInformation {
    override val consentStatus: Int = ConsentInformation.ConsentStatus.UNKNOWN
    override val privacyOptionsRequirementStatus: ConsentInformation.PrivacyOptionsRequirementStatus =
        ConsentInformation.PrivacyOptionsRequirementStatus.UNKNOWN

    override fun canRequestAds(): Boolean = false
    override fun isConsentFormAvailable(): Boolean = false
    override fun reset() {}
    override fun requestConsentInfoUpdate(
        activity: Activity,
        params: ConsentRequestParameters,
        successListener: ConsentInformation.OnConsentInfoUpdateSuccessListener,
        failureListener: ConsentInformation.OnConsentInfoUpdateFailureListener
    ) {
    }
}
