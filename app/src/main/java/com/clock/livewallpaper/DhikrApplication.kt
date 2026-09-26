package com.clock.livewallpaper

import android.app.Application
import com.clock.livewallpaper.core.AppStartup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Offline by construction: there is no network client, no analytics SDK and no crash reporter to
 * initialise here - only the notification channels, the local seeding of the bundled adhkar and
 * the alarms the user has already enabled.
 */
@HiltAndroidApp
class DhikrApplication : Application() {

    @Inject
    lateinit var startup: AppStartup

    override fun onCreate() {
        super.onCreate()
        startup.start()
    }
}
