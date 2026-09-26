package com.clock.livewallpaper.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * SYSTEM_ALERT_WINDOW helpers.
 *
 * The permission is optional for the whole product: every other feature works without it, and the
 * reminder engine falls back to a notification when it is missing or revoked.
 */
object OverlayPermission {

    fun canDraw(context: Context): Boolean = runCatching {
        Settings.canDrawOverlays(context)
    }.getOrDefault(false)

    /** The system screen that grants "الظهور فوق التطبيقات" for this package. */
    fun settingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
