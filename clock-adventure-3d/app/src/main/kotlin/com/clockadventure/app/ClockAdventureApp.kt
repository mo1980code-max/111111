package com.clockadventure.app

import android.app.Application
import com.clockadventure.app.notification.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Hilt builds the whole object graph here (database, DataStore, audio and
 * the repositories), so every screen can simply inject what it needs.
 */
@HiltAndroidApp
class ClockAdventureApp : Application() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        reminderScheduler.createChannel()
        reminderScheduler.scheduleDailyReminder()
    }
}
