package com.clockadventure.app.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.clockadventure.app.MainActivity
import com.clockadventure.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One optional daily reminder at 17:00 ("time for a little clock practise").
 *
 * It is scheduled entirely on the device, stores nothing anywhere else and can be switched off in
 * Settings. On Android 13+ the notification permission gate is handled by the system; when the
 * permission has not been granted the reminder is simply never shown.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.createNotificationChannel(channel)
    }

    fun scheduleDailyReminder() {
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        runCatching {
            alarmManager?.setRepeating(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pending
            )
        }
    }

    fun cancel() {
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        runCatching { alarmManager?.cancel(pending) }
    }

    fun showReminder() {
        if (!canPostNotifications()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        val pending = PendingIntent.getActivity(context, 0, intent, flags)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun pendingIntentImmutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val CHANNEL_ID = "clock_adventure_reminders"
        private const val REQUEST_CODE = 4711
        private const val NOTIFICATION_ID = 4712
    }
}
