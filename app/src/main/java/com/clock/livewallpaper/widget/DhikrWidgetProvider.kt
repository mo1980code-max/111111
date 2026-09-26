package com.clock.livewallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.clock.livewallpaper.R
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.ui.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home-screen dhikr widget.
 *
 * Reads one short dhikr from the local database - never the network - and rotates to a different
 * one every time "ذكر آخر" is tapped, reusing the same recent-history logic as the reminders so
 * the same dhikr does not come back twice in a row.
 */
@AndroidEntryPoint
class DhikrWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var repository: DhikrRepository

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT_DHIKR) {
            render(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        render(context)
    }

    override fun onEnabled(context: Context) {
        render(context)
    }

    private fun render(context: Context) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val manager = AppWidgetManager.getInstance(appContext) ?: return@launch
                val ids = manager.getAppWidgetIds(
                    ComponentName(appContext, DhikrWidgetProvider::class.java)
                )
                if (ids.isEmpty()) return@launch

                repository.ensureSeeded()
                val dhikr = repository.pickForReminder()
                val text = dhikr?.let { repository.shortText(it) }
                    ?: appContext.getString(R.string.widget_empty)

                val views = RemoteViews(appContext.packageName, R.layout.widget_dhikr).apply {
                    setTextViewText(R.id.widget_dhikr_text, text)
                    setOnClickPendingIntent(R.id.widget_next, nextIntent(appContext))
                    setOnClickPendingIntent(
                        R.id.widget_root,
                        WidgetRefresh.openRoute(appContext, Routes.HOME, REQUEST_OPEN)
                    )
                }
                runCatching { manager.updateAppWidget(ids, views) }
            } catch (error: Throwable) {
                // A widget refresh must never crash the host launcher process chain.
            } finally {
                runCatching { pendingResult.finish() }
            }
        }
    }

    private fun nextIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_NEXT,
        Intent(context, DhikrWidgetProvider::class.java).setAction(ACTION_NEXT_DHIKR),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val ACTION_NEXT_DHIKR = "com.clock.livewallpaper.action.WIDGET_NEXT_DHIKR"
        private const val REQUEST_NEXT = 3101
        private const val REQUEST_OPEN = 3102
    }
}
