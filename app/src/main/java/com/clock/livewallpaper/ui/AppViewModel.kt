package com.clock.livewallpaper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The two things the whole app needs before the first frame: the persisted theme and whether
 * onboarding is behind us. The splash screen stays up until [AppUiState.loaded] flips, so the
 * user never sees a light flash before a dark theme, or Home before onboarding.
 */
data class AppUiState(
    val loaded: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false
)

@HiltViewModel
class AppViewModel @Inject constructor(
    settings: SettingsRepository
) : ViewModel() {

    val state: StateFlow<AppUiState> =
        combine(settings.appearance, settings.onboardingCompleted) { appearance, onboarding ->
            AppUiState(
                loaded = true,
                themeMode = appearance.themeMode,
                onboardingCompleted = onboarding
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppUiState()
        )
}
