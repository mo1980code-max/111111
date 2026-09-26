package com.clock.livewallpaper.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    /** Persists the flag first, then lets the caller navigate, so a restart cannot repeat it. */
    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setOnboardingCompleted(true)
            onDone()
        }
    }
}
