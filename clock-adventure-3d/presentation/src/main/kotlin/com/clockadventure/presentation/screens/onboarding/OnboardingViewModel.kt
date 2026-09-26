package com.clockadventure.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockadventure.domain.model.AppSettings
import com.clockadventure.domain.model.MascotId
import com.clockadventure.domain.repository.AudioController
import com.clockadventure.domain.repository.ProgressRepository
import com.clockadventure.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The four pages of the first run introduction. */
enum class OnboardingPage { WELCOME, HANDS, REWARDS, OFFLINE }

data class OnboardingUiState(
    val settings: AppSettings = AppSettings(),
    val page: OnboardingPage = OnboardingPage.WELCOME
) {
    val isLast: Boolean get() = page == OnboardingPage.OFFLINE
    val index: Int get() = OnboardingPage.entries.indexOf(page)
    val pageCount: Int get() = OnboardingPage.entries.size
}

/**
 * Drives the first run introduction: three short explanations and a character choice, shown once.
 *
 * When the child presses the last button the intro is marked as seen in the settings *and* in the
 * profile row, so it never appears again - not on the next start, and not after a backup restore
 * that only kept the database.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
    private val audio: AudioController
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _page = MutableStateFlow(OnboardingPage.WELCOME)
    val page: StateFlow<OnboardingPage> = _page.asStateFlow()

    fun next(onFinished: () -> Unit) {
        val current = _page.value
        val values = OnboardingPage.entries
        val nextIndex = (values.indexOf(current) + 1).coerceAtMost(values.lastIndex)
        if (current == OnboardingPage.OFFLINE || nextIndex == values.indexOf(current)) {
            finish(onFinished)
        } else {
            audio.button()
            _page.value = values[nextIndex]
        }
    }

    fun back() {
        val values = OnboardingPage.entries
        val previous = (values.indexOf(_page.value) - 1).coerceAtLeast(0)
        audio.button()
        _page.value = values[previous]
    }

    fun selectCharacter(character: MascotId) {
        audio.button()
        viewModelScope.launch {
            settingsRepository.update { it.copy(character = character) }
        }
    }

    fun skip(onFinished: () -> Unit) = finish(onFinished)

    private fun finish(onFinished: () -> Unit) {
        audio.celebrate()
        viewModelScope.launch {
            settingsRepository.update { it.copy(hasSeenIntro = true) }
            progressRepository.markIntroSeen()
            onFinished()
        }
    }
}
