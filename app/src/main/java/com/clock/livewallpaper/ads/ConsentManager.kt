package com.clock.livewallpaper.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the User Messaging Platform currently allows. The rest of the ad layer reads nothing else:
 * no cached consent string, no preference of our own, no "it is a debug build so it is fine".
 */
data class ConsentState(
    /** The UMP flow finished in this process; before that only the cached answer is known. */
    val gathered: Boolean = false,
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false
)

/**
 * Google UMP, wrapped once.
 *
 * Rules this class exists to enforce:
 *
 * * the consent information is refreshed with a **foreground Activity**, which is passed in as a
 *   parameter and never stored - the singleton only ever holds the application context;
 * * two consent updates, or two forms, can never run at the same time ([updateInFlight] /
 *   [formInFlight]);
 * * the answer is published as state, so `canRequestAds` decides whether anything is requested;
 * * the privacy options form is offered exactly when UMP says an entry point is required;
 * * debug builds behave like release builds - the consent flow is never short-circuited.
 */
@Singleton
class ConsentManager @Inject constructor(@ApplicationContext context: Context) {

    // UserMessagingPlatform keeps one instance per process and only needs the application
    // context, so no Activity can survive inside this singleton.
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val updateInFlight = AtomicBoolean(false)
    private val formInFlight = AtomicBoolean(false)

    private val _state = MutableStateFlow(ConsentState())
    val state: StateFlow<ConsentState> = _state.asStateFlow()

    private val _formVisible = MutableStateFlow(false)

    /** True while a UMP form owns the screen; no full-screen ad may race it. */
    val formVisible: StateFlow<Boolean> = _formVisible.asStateFlow()

    /**
     * Refreshes the consent information and shows the consent form when UMP requires one.
     *
     * Safe to call on every foreground: the in-flight guard turns the extra calls into no-ops.
     */
    fun gather(activity: Activity) {
        publish()
        if (activity.isFinishing || activity.isDestroyed) return
        if (!updateInFlight.compareAndSet(false, true)) return

        AdLog.d("UMP requestConsentInfoUpdate started")
        val parameters = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                AdLog.d(
                    "UMP consent info updated" +
                        " status=${consentInformation.consentStatus}" +
                        " canRequestAds=${consentInformation.canRequestAds()}" +
                        " privacyOptions=${consentInformation.privacyOptionsRequirementStatus}"
                )
                publish()
                loadAndShowFormIfRequired(activity)
            },
            { error ->
                // UMP falls back to the consent of the previous session; the next foreground
                // retries, so a flaky network never blocks the app.
                AdLog.w(
                    "UMP requestConsentInfoUpdate failed" +
                        " code=${error.errorCode} message=${error.message}"
                )
                updateInFlight.set(false)
                publish()
            }
        )
    }

    /** The privacy options form, shown only from the entry point the user taps in settings. */
    fun showPrivacyOptions(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!formInFlight.compareAndSet(false, true)) return

        AdLog.d("UMP showPrivacyOptionsForm started")
        _formVisible.value = true
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError == null) {
                AdLog.d("UMP privacy options dismissed canRequestAds=${consentInformation.canRequestAds()}")
            } else {
                AdLog.w(
                    "UMP privacy options failed" +
                        " code=${formError.errorCode} message=${formError.message}"
                )
            }
            formInFlight.set(false)
            _formVisible.value = false
            publish()
        }
    }

    private fun loadAndShowFormIfRequired(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            updateInFlight.set(false)
            publish(gathered = true)
            return
        }
        if (!formInFlight.compareAndSet(false, true)) {
            // A form is already on screen; the update is finished either way.
            updateInFlight.set(false)
            publish()
            return
        }

        AdLog.d("UMP loadAndShowConsentFormIfRequired started")
        _formVisible.value = true
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            if (formError == null) {
                AdLog.d("UMP consent gathering complete canRequestAds=${consentInformation.canRequestAds()}")
            } else {
                AdLog.w(
                    "UMP consent form finished with an error" +
                        " code=${formError.errorCode} message=${formError.message}"
                )
            }
            formInFlight.set(false)
            updateInFlight.set(false)
            _formVisible.value = false
            publish(gathered = true)
        }
    }

    private fun publish(gathered: Boolean = _state.value.gathered) {
        _state.value = ConsentState(
            gathered = gathered,
            canRequestAds = consentInformation.canRequestAds(),
            privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        )
    }
}
