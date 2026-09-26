package com.clock.livewallpaper.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.clock.livewallpaper.MainActivity
import com.clock.livewallpaper.R
import com.clock.livewallpaper.reminder.DailyReminderKind
import com.clock.livewallpaper.ui.navigation.Routes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All notification work: channels, the Android 13+ permission check, explicit PendingIntents and
 * the routing that opens the right Compose destination.
 *
 * Nothing here is required for the app to work - if the user denies notifications, every call
 * simply returns false and the rest of the product keeps running.
 */
@Singleton
class DhikrNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val systemManager = context.getSystemService(NotificationManager::class.java) ?: return

        val dhikrChannel = NotificationChannel(
            CHANNEL_DHIKR,
            context.getString(R.string.channel_dhikr_name),
            // Low: the periodic reminder must never buzz or take over the screen.
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_dhikr_desc)
            setShowBadge(false)
            enableVibration(false)
        }

        val dailyChannel = NotificationChannel(
            CHANNEL_DAILY,
            context.getString(R.string.channel_daily_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_daily_desc)
            setShowBadge(true)
        }

        runCatching { systemManager.createNotificationChannel(dhikrChannel) }
        runCatching { systemManager.createNotificationChannel(dailyChannel) }
    }

    /** True when the app may post: runtime permission granted (13+) and notifications enabled. */
    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return runCatching { manager.areNotificationsEnabled() }.getOrDefault(false)
    }

    /** Fallback used when the floating card cannot be shown. */
    fun showDhikr(text: String): Boolean {
        if (!canPost()) return false
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_DHIKR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_dhikr_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(routeIntent(Routes.HOME, REQUEST_DHIKR))
            .build()
        return post(NOTIFICATION_DHIKR, notification)
    }

    fun showDaily(kind: DailyReminderKind): Boolean {
        if (!canPost()) return false
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(kind.titleRes))
            .setContentText(context.getString(kind.textRes))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(context.getString(kind.textRes))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(routeIntent(kind.route, kind.requestCode))
            .build()
        return post(kind.notificationId, notification)
    }

    private fun post(id: Int, notification: android.app.Notification): Boolean = runCatching {
        manager.notify(id, notification)
        true
    }.getOrDefault(false)

    private fun routeIntent(route: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            MainActivity.routeIntent(context, route),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val CHANNEL_DHIKR = "dhikr_reminders"
        const val CHANNEL_DAILY = "daily_adhkar"

        const val NOTIFICATION_DHIKR = 2301
        private const val REQUEST_DHIKR = 2104
    }
}
