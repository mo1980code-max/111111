// The parts of the app the advertising layer touches, declared with the same signatures as the
// real sources so the type-check sees exactly what the compiler would.
//
// Keeping them here instead of compiling the real files keeps the scope of this check on the ad
// layer: SettingsRepository drags in DataStore and Room, DhikrCard drags in the whole theme, and
// none of that is what these stubs are meant to prove. The signatures below are copied verbatim
// from:
//   app/src/main/java/com/clock/livewallpaper/di/AppModule.kt
//   app/src/main/java/com/clock/livewallpaper/data/prefs/SettingsRepository.kt
//   app/src/main/java/com/clock/livewallpaper/ui/components/Surfaces.kt
//   app/src/main/java/com/clock/livewallpaper/ui/components/SettingsComponents.kt
// and tests/test_api_contracts.py checks every call site against the real declarations.
package com.clock.livewallpaper.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
