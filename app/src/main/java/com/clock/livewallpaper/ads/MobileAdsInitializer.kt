package com.clock.livewallpaper.ads

import android.content.Context
import com.clock.livewallpaper.di.ApplicationScope
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `MobileAds.initialize`, exactly once per process and never before it is allowed.
 *
 * The SDK is not started in `Application.onCreate`: it is started by [AdsCoordinator] the moment
 * UMP reports `canRequestAds == true`, which may be at the first launch (cached consent) or after
 * the consent form is dismissed. Initialisation does disk and network work, so it runs off the
 * main thread as Google recommends, and [initialized] lets Compose wait for it without polling.
 *
 * "Initialised" only means the SDK is up - it says nothing about an ad being available.
 */
@Singleton
class MobileAdsInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val started = AtomicBoolean(false)

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    /** Idempotent and lifecycle-safe: repeated calls after the first one do nothing. */
    fun ensureInitialized() {
        if (!started.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            AdLog.d("MobileAds.initialize() requested")
            MobileAds.initialize(context) { status ->
                val adapters = status.adapterStatusMap.entries.joinToString { entry ->
                    "${entry.key}=${entry.value.initializationState}"
                }
                AdLog.d("MobileAds SDK initialized (no ad requested yet) adapters=[$adapters]")
                _initialized.value = true
            }
        }
    }
}
