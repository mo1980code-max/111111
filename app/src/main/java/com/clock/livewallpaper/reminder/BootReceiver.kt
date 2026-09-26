package com.clock.livewallpaper.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clock.livewallpaper.widget.WidgetRefresh
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restores the schedules the user actually enabled: after a reboot, after the app was replaced and
 * after the system clock or the timezone changed (the daily reminders are wall-clock alarms, so
 * they have to be recomputed against the new local time).
 *
 * Each schedule is restored independently from its own stored switch, nothing is duplicated
 * (AlarmManager replaces an alarm that shares a PendingIntent, and an already armed periodic
 * chain is left untouched) and no UI is ever launched.
 *
 * LOCKED_BOOT_COMPLETED is deliberately NOT handled: the app is not directBootAware, so the
 * receiver would not be started before the user unlocks anyway, and the settings it needs live in
 * credential-encrypted storage which is unreadable at that point. BOOT_COMPLETED is delivered
 * right after the unlock and is the correct trigger. All four actions accepted below are exempt
 * from the implicit-broadcast restrictions, so the manifest registration keeps working.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var presenter: ReminderPresenter

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in HANDLED_ACTIONS) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                presenter.restoreSchedules()
                WidgetRefresh.request(appContext)
            } catch (error: Throwable) {
                // Never crash during boot.
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }

    private companion object {

        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
