package com.clock.livewallpaper.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Periodic dhikr reminder.
 *
 * goAsync() keeps the broadcast alive while the card is added / the notification is posted; the
 * window itself survives afterwards because it belongs to WindowManager, not to this receiver.
 */
@AndroidEntryPoint
class DhikrReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var presenter: ReminderPresenter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null && intent.action != ACTION_DHIKR_REMINDER) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                presenter.onPeriodicAlarm()
            } catch (error: Throwable) {
                // A reminder must never take the process down.
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }

    companion object {
        const val ACTION_DHIKR_REMINDER = "com.clock.livewallpaper.action.DHIKR_REMINDER"
    }
}
