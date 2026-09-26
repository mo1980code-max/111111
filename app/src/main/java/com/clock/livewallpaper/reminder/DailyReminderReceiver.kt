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

/** Morning / evening / Friday reminders; the kind travels in the alarm intent. */
@AndroidEntryPoint
class DailyReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var presenter: ReminderPresenter

    override fun onReceive(context: Context, intent: Intent) {
        val kind = DailyReminderKind.fromKey(intent.getStringExtra(EXTRA_KIND)) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                presenter.onDailyAlarm(kind)
            } catch (error: Throwable) {
                // Ignored on purpose: a missed notification must not crash the app.
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.clock.livewallpaper.action.DAILY_REMINDER"
        const val EXTRA_KIND = "com.clock.livewallpaper.extra.DAILY_KIND"
    }
}
