package com.clock.livewallpaper.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The only ad state Compose ever sees. */
data class AdsUiState(
    val adsReady: Boolean = false,
    val privacyOptionsRequired: Boolean = false
)

/**
 * The bridge between the ad singletons and Compose.
 *
 * Screens never touch the Mobile Ads SDK; they place a slot, and the slot reads this. The view
 * model also owns the lifetime of the one native ad a screen may show: because it outlives the
 * composition, scrolling the card in and out of a lazy list re-uses the ad instead of requesting
 * a new one every time, and [onCleared] destroys it when the screen is really gone.
 */
@HiltViewModel
class AdsViewModel @Inject constructor(
    private val coordinator: AdsCoordinator,
    private val nativeAds: NativeAdManager
) : ViewModel() {

    private val _nativeAd = MutableStateFlow<NativeAd?>(null)

    /** The native ad of this screen, or null while there is none to show. */
    val nativeAd: StateFlow<NativeAd?> = _nativeAd.asStateFlow()

    private var nativeRequest: NativeAdRequest? = null

    val state: StateFlow<AdsUiState> = combine(
        coordinator.adsReady,
        coordinator.privacyOptionsRequired
    ) { ready, privacyOptions ->
        AdsUiState(adsReady = ready, privacyOptionsRequired = privacyOptions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdsUiState()
    )

    /** Google's privacy options form, opened from the settings row UMP asked for. */
    fun showPrivacyOptions(activity: Activity) {
        coordinator.showPrivacyOptions(activity)
    }

    /** Requests the screen's native ad once; calls made while one is loading or loaded do nothing. */
    fun loadNativeAd() {
        if (!coordinator.adsReady.value) return
        if (_nativeAd.value != null || nativeRequest != null) return
        nativeRequest = nativeAds.load { ad ->
            nativeRequest = null
            _nativeAd.value?.destroy()
            _nativeAd.value = ad
        }
    }

    override fun onCleared() {
        super.onCleared()
        nativeRequest?.cancel()
        nativeRequest = null
        _nativeAd.value?.destroy()
        _nativeAd.value = null
    }
}
