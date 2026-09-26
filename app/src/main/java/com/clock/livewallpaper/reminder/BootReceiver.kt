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
 * Restores the schedules the user actually enabled after a reboot or an app update.
 *
 * Each schedule is restored independently from its own stored switch, nothing is duplicated
 * (AlarmManager replaces an alarm that shares a PendingIntent) and no UI is ever launched.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var presenter: ReminderPresenter

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
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
}
