package com.clock.livewallpaper.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.AlarmManagerCompat
import com.clock.livewallpaper.data.prefs.ReminderSettings
import com.clock.livewallpaper.data.prefs.SettingsSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All AlarmManager work.
 *
 * Scheduling policy: INEXACT, permission-free alarms (`setAndAllowWhileIdle`). The app never asks
 * for SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM, and never pretends a PeriodicWorkRequest can fire
 * every five minutes. Doze may therefore delay a reminder - that is accepted and explained to the
 * user in the reminder settings instead of being worked around.
 *
 * The periodic reminder is a self-rearming chain: every delivery schedules the next one, which is
 * also what makes a changed interval take effect immediately. That chain is armed on the elapsed
 * realtime clock, so moving the wall clock or crossing a timezone cannot skip it or fire it early;
 * the daily reminders are wall-clock alarms by definition and are simply recomputed when the
 * system reports a time or timezone change.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun alarmManager(): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    // ------------------------------------------------------------- periodic

    fun schedulePeriodic(intervalMinutes: Int) {
        val minutes = ReminderSettings.sanitizeInterval(intervalMinutes)
        val triggerAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        setElapsedAlarm(triggerAt, periodicIntent())
    }

    fun cancelPeriodic() {
        runCatching {
            val operation = periodicIntent()
            alarmManager()?.cancel(operation)
            // Drops the PendingIntent record too, so [isPeriodicPending] cannot report a chain
            // that was just switched off.
            operation.cancel()
        }
    }

    /**
     * True when a periodic delivery is already armed for this process.
     *
     * FLAG_NO_CREATE asks the system whether the record still exists; it is gone after a reboot,
     * after the package was replaced and after the chain was cancelled, which is exactly when the
     * chain has to be armed again.
     */
    fun isPeriodicPending(): Boolean = PendingIntent.getBroadcast(
        context,
        REQUEST_PERIODIC,
        periodicBaseIntent(),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) != null

    // ---------------------------------------------------------------- daily

    fun scheduleDaily(kind: DailyReminderKind, minuteOfDay: Int) {
        setAlarm(nextTriggerAt(kind, minuteOfDay), dailyIntent(kind))
    }

    fun cancelDaily(kind: DailyReminderKind) {
        runCatching {
            val operation = dailyIntent(kind)
            alarmManager()?.cancel(operation)
            operation.cancel()
        }
    }

    /**
     * Applies a full settings snapshot - used on boot, on app start and after any change.
     *
     * [keepPendingChain] is set by the callers that only restore state (process start, boot,
     * time changes). Without it every cold start would push the next periodic reminder a full
     * interval into the future, so a user who opens the app often would never be reminded.
     * A real settings change leaves it false, which re-arms the chain immediately.
     */
    fun apply(snapshot: SettingsSnapshot, keepPendingChain: Boolean = false) {
        if (snapshot.reminder.enabled) {
            if (!keepPendingChain || !isPeriodicPending()) {
                schedulePeriodic(snapshot.reminder.intervalMinutes)
            }
        } else {
            cancelPeriodic()
        }
        applyDaily(DailyReminderKind.MORNING, snapshot.morning.enabled, snapshot.morning.minuteOfDay)
        applyDaily(DailyReminderKind.EVENING, snapshot.evening.enabled, snapshot.evening.minuteOfDay)
        applyDaily(DailyReminderKind.FRIDAY, snapshot.friday.enabled, snapshot.friday.minuteOfDay)
    }

    fun applyDaily(kind: DailyReminderKind, enabled: Boolean, minuteOfDay: Int) {
        if (enabled) scheduleDaily(kind, minuteOfDay) else cancelDaily(kind)
    }

    /**
     * Next wall-clock moment for a daily reminder.
     *
     * Today when the time is still ahead, otherwise the next valid day. Friday reminders jump to
     * the coming Friday; Calendar arithmetic keeps the result correct across DST changes.
     */
    fun nextTriggerAt(
        kind: DailyReminderKind,
        minuteOfDay: Int,
        from: Calendar = Calendar.getInstance()
    ): Long {
        val target = from.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        target.set(Calendar.MINUTE, minuteOfDay % 60)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        if (kind.weeklyOnFriday) {
            var guard = 0
            while ((target.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY ||
                    target.timeInMillis <= from.timeInMillis) && guard < 8
            ) {
                target.add(Calendar.DAY_OF_MONTH, 1)
                guard++
            }
        } else if (target.timeInMillis <= from.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis
    }

    // --------------------------------------------------------------- plumbing

    /** Wall-clock alarm, for the reminders the user picked a time of day for. */
    private fun setAlarm(triggerAtMillis: Long, operation: PendingIntent) {
        setAlarm(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
    }

    /** Elapsed-realtime alarm, for interval based reminders: immune to clock and timezone edits. */
    private fun setElapsedAlarm(triggerAtMillis: Long, operation: PendingIntent) {
        setAlarm(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, operation)
    }

    private fun setAlarm(type: Int, triggerAtMillis: Long, operation: PendingIntent) {
        val manager = alarmManager() ?: return
        runCatching {
            AlarmManagerCompat.setAndAllowWhileIdle(manager, type, triggerAtMillis, operation)
        }
    }

    private fun periodicBaseIntent(): Intent =
        Intent(context, DhikrReminderReceiver::class.java)
            .setAction(DhikrReminderReceiver.ACTION_DHIKR_REMINDER)

    private fun periodicIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_PERIODIC,
        periodicBaseIntent(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun dailyIntent(kind: DailyReminderKind): PendingIntent = PendingIntent.getBroadcast(
        context,
        kind.requestCode,
        Intent(context, DailyReminderReceiver::class.java)
            .setAction(DailyReminderReceiver.ACTION_DAILY_REMINDER)
            .putExtra(DailyReminderReceiver.EXTRA_KIND, kind.key),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val REQUEST_PERIODIC = 2100
    }
}
