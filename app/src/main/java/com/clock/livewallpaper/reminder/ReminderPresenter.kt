package com.clock.livewallpaper.reminder

import android.content.Context
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.notification.DhikrNotifier
import com.clock.livewallpaper.overlay.OverlayManager
import com.clock.livewallpaper.overlay.OverlayPermission
import com.clock.livewallpaper.widget.WidgetRefresh
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What actually happens when a reminder becomes due. Kept out of the receivers so the decision
 * chain is testable and identical for the periodic alarm, the boot restore and the "try it now"
 * button in settings.
 */
@Singleton
class ReminderPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val dhikrRepository: DhikrRepository,
    private val overlayManager: OverlayManager,
    private val notifier: DhikrNotifier,
    private val scheduler: ReminderScheduler
) {

    /**
     * 1 reminder enabled? 2 quiet hours? 3 pick an eligible dhikr avoiding the recent ones,
     * 4 floating card when allowed, 5 notification fallback otherwise, 6 always keep the chain
     * alive. No network is touched at any step.
     */
    suspend fun onPeriodicAlarm() {
        val snapshot = settings.snapshot()
        if (!snapshot.reminder.enabled) {
            scheduler.cancelPeriodic()
            return
        }
        // Re-arm first: whatever happens below, the next reminder is already booked.
        scheduler.schedulePeriodic(snapshot.reminder.intervalMinutes)

        if (snapshot.quietHours.isQuietAt(currentMinuteOfDay())) return

        dhikrRepository.ensureSeeded()
        val dhikr = dhikrRepository.pickForReminder() ?: return
        val text = dhikrRepository.shortText(dhikr)

        val shownAsCard = snapshot.overlay.enabled &&
            OverlayPermission.canDraw(context) &&
            overlayManager.showDhikrAsync(text, snapshot.overlay)

        if (!shownAsCard) {
            notifier.showDhikr(text)
        }
        WidgetRefresh.request(context)
    }

    suspend fun onDailyAlarm(kind: DailyReminderKind) {
        val snapshot = settings.snapshot()
        val daily = when (kind) {
            DailyReminderKind.MORNING -> snapshot.morning
            DailyReminderKind.EVENING -> snapshot.evening
            DailyReminderKind.FRIDAY -> snapshot.friday
        }
        if (!daily.enabled) {
            scheduler.cancelDaily(kind)
            return
        }
        // Book the next occurrence before showing anything.
        scheduler.scheduleDaily(kind, daily.minuteOfDay)
        notifier.showDaily(kind)
    }

    /** Re-arms every enabled schedule; used after boot, package replace and app start. */
    suspend fun restoreSchedules() {
        // keepPendingChain: a restore must never move a reminder that is already armed - only the
        // schedules the system actually dropped (reboot, package replace) are armed again.
        scheduler.apply(settings.snapshot(), keepPendingChain = true)
    }

    /** "تجربة البطاقة الآن" in overlay settings - same renderer, same settings model. */
    suspend fun previewOverlay(): Boolean {
        val overlay = settings.snapshot().overlay
        if (!OverlayPermission.canDraw(context)) return false
        dhikrRepository.ensureSeeded()
        val dhikr = dhikrRepository.pickForReminder() ?: return false
        return overlayManager.showDhikrAsync(dhikrRepository.shortText(dhikr), overlay)
    }

    private fun currentMinuteOfDay(calendar: Calendar = Calendar.getInstance()): Int =
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}
