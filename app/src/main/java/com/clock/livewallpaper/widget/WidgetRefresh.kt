package com.clock.livewallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.clock.livewallpaper.MainActivity

/**
 * Widget helpers shared by both providers.
 *
 * Widgets declare updatePeriodMillis = 0: the system never polls them. They refresh only when
 * something real happened (a reminder fired, the user asked for another dhikr, a reboot), which
 * keeps the battery cost at zero.
 */
object WidgetRefresh {

    fun request(context: Context) {
        broadcastUpdate(context, DhikrWidgetProvider::class.java)
        broadcastUpdate(context, QuickActionsWidgetProvider::class.java)
    }

    private fun broadcastUpdate(context: Context, provider: Class<out AppWidgetProvider>) {
        runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isEmpty()) return
            val intent = Intent(context, provider)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    /** Explicit, immutable PendingIntent that opens a Compose destination. */
    fun openRoute(context: Context, route: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            MainActivity.routeIntent(context, route),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
