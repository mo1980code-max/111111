package com.clock.livewallpaper.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.clock.livewallpaper.R
import com.clock.livewallpaper.ui.navigation.Routes

/** الصباح / المساء / المسبحة - three explicit PendingIntents into the Compose destinations. */
class QuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_actions).apply {
            setOnClickPendingIntent(
                R.id.widget_action_morning,
                WidgetRefresh.openRoute(context, Routes.MORNING, REQUEST_MORNING)
            )
            setOnClickPendingIntent(
                R.id.widget_action_evening,
                WidgetRefresh.openRoute(context, Routes.EVENING, REQUEST_EVENING)
            )
            setOnClickPendingIntent(
                R.id.widget_action_tasbeeh,
                WidgetRefresh.openRoute(context, Routes.TASBEEH, REQUEST_TASBEEH)
            )
        }
        runCatching { appWidgetManager.updateAppWidget(appWidgetIds, views) }
    }

    companion object {
        private const val REQUEST_MORNING = 3201
        private const val REQUEST_EVENING = 3202
        private const val REQUEST_TASBEEH = 3203
    }
}
