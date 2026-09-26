package com.clock.livewallpaper.ui.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * POST_NOTIFICATIONS handling.
 *
 * Notifications are the fallback path of the reminder engine, never a requirement: if the user
 * says no, the app keeps working and only the floating card is used.
 */
object NotificationPermission {

    const val PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

    /** Only Android 13+ has a runtime permission; before that the channel switch is enough. */
    fun needsRuntimeRequest(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun isGranted(context: Context): Boolean {
        val runtimeGranted = if (needsRuntimeRequest()) {
            ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        // The user can also switch the app's notifications off entirely in system settings.
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** System screen where notifications can be re-enabled after a permanent denial. */
    fun settingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
