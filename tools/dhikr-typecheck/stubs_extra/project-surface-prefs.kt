// See project-surface.kt: signature copied verbatim from data/prefs/SettingsRepository.kt.
package com.clock.livewallpaper.data.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor() {
    val onboardingCompleted: Flow<Boolean> = flowOf(false)
}
