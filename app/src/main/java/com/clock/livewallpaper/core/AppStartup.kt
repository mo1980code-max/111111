package com.clock.livewallpaper.core

import android.content.Context
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.di.ApplicationScope
import com.clock.livewallpaper.notification.DhikrNotifier
import com.clock.livewallpaper.reminder.ReminderScheduler
import com.clock.livewallpaper.widget.WidgetRefresh
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the process needs once, kept out of [com.clock.livewallpaper.DhikrApplication] so the
 * qualified application scope is constructor-injected instead of field-injected.
 *
 * Nothing here blocks the main thread: the channels are cheap and registered synchronously, and
 * the database seeding plus the alarm bookkeeping run on the application scope.
 */
@Singleton
class AppStartup @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val repository: DhikrRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: DhikrNotifier
) {

    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return

        notifier.ensureChannels()

        scope.launch {
            runCatching {
                // Idempotent: seeds only when the stored seed version is older than the bundled one.
                repository.ensureSeeded()
                // Re-applies the user's own schedule, also after an app update or a settings restore.
                scheduler.apply(settings.snapshot())
                WidgetRefresh.request(context)
            }
        }
    }
}
