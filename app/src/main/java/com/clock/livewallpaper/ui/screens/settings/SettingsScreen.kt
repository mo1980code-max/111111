package com.clock.livewallpaper.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.data.prefs.AppearanceSettings
import com.clock.livewallpaper.data.prefs.ReminderSettings
import com.clock.livewallpaper.data.prefs.ThemeMode
import com.clock.livewallpaper.overlay.OverlayPermission
import com.clock.livewallpaper.reminder.DailyReminderKind
import com.clock.livewallpaper.ui.LocalSnackbarHostState
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.ChipFlowRow
import com.clock.livewallpaper.ui.components.InlineNote
import com.clock.livewallpaper.ui.components.IntervalPromptDialog
import com.clock.livewallpaper.ui.components.PermissionCard
import com.clock.livewallpaper.ui.components.SelectableChip
import com.clock.livewallpaper.ui.components.SettingsChoiceRow
import com.clock.livewallpaper.ui.components.SettingsDivider
import com.clock.livewallpaper.ui.components.SettingsFootnote
import com.clock.livewallpaper.ui.components.SettingsNavRow
import com.clock.livewallpaper.ui.components.SettingsSection
import com.clock.livewallpaper.ui.components.SettingsSliderRow
import com.clock.livewallpaper.ui.components.SettingsSwitchRow
import com.clock.livewallpaper.ui.components.SettingsValueRow
import com.clock.livewallpaper.ui.components.TimePromptDialog
import com.clock.livewallpaper.ui.util.NotificationPermission
import kotlinx.coroutines.launch

private enum class TimeTarget { MORNING, EVENING, FRIDAY, QUIET_START, QUIET_END }

/**
 * One unified settings screen: the floating card, the reminders, the daily and Friday adhkar,
 * quiet hours, appearance, the tasbeeh haptic and the two legal screens.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    var overlayGranted by remember { mutableStateOf(OverlayPermission.canDraw(context)) }
    var notificationsGranted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    val notificationDenied = stringResource(R.string.notification_permission_denied)

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted && NotificationPermission.isGranted(context)
        if (!granted) {
            scope.launch { snackbarHostState.showSnackbar(notificationDenied) }
        }
    }

    LifecycleResumeEffect(Unit) {
        overlayGranted = OverlayPermission.canDraw(context)
        notificationsGranted = NotificationPermission.isGranted(context)
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.settings_title), onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 36.dp)
        ) {

            // ------------------------------------------------- floating card
            item(key = "overlay") {
                SettingsSection(title = stringResource(R.string.settings_section_overlay)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_overlay_enabled),
                        subtitle = stringResource(R.string.settings_overlay_enabled_desc),
                        iconRes = R.drawable.ic_card,
                        checked = state.overlay.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.setOverlayEnabled(enabled)
                            if (enabled && !overlayGranted) {
                                runCatching {
                                    context.startActivity(OverlayPermission.settingsIntent(context))
                                }
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_overlay_appearance),
                        subtitle = stringResource(R.string.settings_overlay_appearance_desc),
                        iconRes = R.drawable.ic_palette,
                        onClick = onOpenOverlaySettings
                    )
                    SettingsFootnote(
                        text = stringResource(
                            if (overlayGranted) {
                                R.string.overlay_permission_granted
                            } else {
                                R.string.overlay_permission_missing
                            }
                        )
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            if (state.overlay.enabled && !overlayGranted) {
                item(key = "overlay_permission") {
                    PermissionCard(
                        iconRes = R.drawable.ic_shield,
                        title = stringResource(R.string.overlay_permission_title),
                        text = stringResource(R.string.overlay_permission_text),
                        note = stringResource(R.string.overlay_permission_privacy),
                        ctaText = stringResource(R.string.overlay_permission_cta),
                        onCta = {
                            runCatching {
                                context.startActivity(OverlayPermission.settingsIntent(context))
                            }
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            // ---------------------------------------------------- reminders
            item(key = "reminders") {
                SettingsSection(title = stringResource(R.string.settings_section_reminders)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_reminder_enabled),
                        subtitle = stringResource(R.string.settings_reminder_enabled_desc),
                        iconRes = R.drawable.ic_bell,
                        checked = state.reminder.enabled,
                        onCheckedChange = viewModel::setReminderEnabled
                    )
                    SettingsDivider()
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_reminder_interval),
                        subtitle = ArabicText.duration(context, state.reminder.intervalMinutes),
                        iconRes = R.drawable.ic_clock
                    ) {
                        ChipFlowRow {
                            ReminderSettings.PRESET_MINUTES.forEach { minutes ->
                                SelectableChip(
                                    label = ArabicText.duration(context, minutes),
                                    selected = state.reminder.intervalMinutes == minutes,
                                    onClick = { viewModel.setReminderInterval(minutes) }
                                )
                            }
                            SelectableChip(
                                label = stringResource(R.string.home_reminder_custom),
                                selected = !ReminderSettings.PRESET_MINUTES.contains(
                                    state.reminder.intervalMinutes
                                ),
                                onClick = { showIntervalDialog = true }
                            )
                        }
                    }
                    SettingsDivider()
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        InlineNote(text = stringResource(R.string.settings_reminder_inexact_note))
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            if (!notificationsGranted) {
                item(key = "notification_permission") {
                    PermissionCard(
                        iconRes = R.drawable.ic_bell,
                        title = stringResource(R.string.notification_permission_title),
                        text = stringResource(R.string.notification_permission_text),
                        ctaText = stringResource(R.string.notification_permission_cta),
                        onCta = {
                            if (NotificationPermission.needsRuntimeRequest()) {
                                notificationLauncher.launch(NotificationPermission.PERMISSION)
                            } else {
                                runCatching {
                                    context.startActivity(
                                        NotificationPermission.settingsIntent(context)
                                    )
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            // --------------------------------------------- morning / evening
            item(key = "daily") {
                SettingsSection(title = stringResource(R.string.settings_section_daily)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_morning_enabled),
                        subtitle = stringResource(R.string.settings_morning_desc),
                        iconRes = R.drawable.ic_sun,
                        checked = state.morning.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.setDailyReminder(
                                DailyReminderKind.MORNING,
                                enabled,
                                state.morning.minuteOfDay
                            )
                        }
                    )
                    SettingsValueRow(
                        title = stringResource(R.string.settings_reminder_time),
                        value = ArabicText.time(context, state.morning.minuteOfDay),
                        iconRes = R.drawable.ic_clock,
                        onClick = { timeTarget = TimeTarget.MORNING }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_evening_enabled),
                        subtitle = stringResource(R.string.settings_evening_desc),
                        iconRes = R.drawable.ic_moon,
                        checked = state.evening.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.setDailyReminder(
                                DailyReminderKind.EVENING,
                                enabled,
                                state.evening.minuteOfDay
                            )
                        }
                    )
                    SettingsValueRow(
                        title = stringResource(R.string.settings_reminder_time),
                        value = ArabicText.time(context, state.evening.minuteOfDay),
                        iconRes = R.drawable.ic_clock,
                        onClick = { timeTarget = TimeTarget.EVENING }
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // ----------------------------------------------------- friday
            item(key = "friday") {
                SettingsSection(title = stringResource(R.string.settings_section_friday)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_friday_enabled),
                        subtitle = stringResource(R.string.settings_friday_desc),
                        iconRes = R.drawable.ic_mosque,
                        checked = state.friday.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.setDailyReminder(
                                DailyReminderKind.FRIDAY,
                                enabled,
                                state.friday.minuteOfDay
                            )
                        }
                    )
                    SettingsValueRow(
                        title = stringResource(R.string.settings_reminder_time),
                        value = ArabicText.time(context, state.friday.minuteOfDay),
                        iconRes = R.drawable.ic_clock,
                        onClick = { timeTarget = TimeTarget.FRIDAY }
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // ------------------------------------------------- quiet hours
            item(key = "quiet") {
                SettingsSection(title = stringResource(R.string.settings_section_quiet)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_quiet_enabled),
                        subtitle = stringResource(R.string.settings_quiet_desc),
                        iconRes = R.drawable.ic_quiet,
                        checked = state.quietHours.enabled,
                        onCheckedChange = viewModel::setQuietHoursEnabled
                    )
                    SettingsValueRow(
                        title = stringResource(R.string.settings_quiet_from),
                        value = ArabicText.time(context, state.quietHours.startMinute),
                        onClick = { timeTarget = TimeTarget.QUIET_START }
                    )
                    SettingsValueRow(
                        title = stringResource(R.string.settings_quiet_to),
                        value = ArabicText.time(context, state.quietHours.endMinute),
                        onClick = { timeTarget = TimeTarget.QUIET_END }
                    )
                    SettingsFootnote(
                        text = stringResource(
                            R.string.settings_quiet_summary,
                            ArabicText.time(context, state.quietHours.startMinute),
                            ArabicText.time(context, state.quietHours.endMinute)
                        )
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // -------------------------------------------------- appearance
            item(key = "appearance") {
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_theme),
                        iconRes = R.drawable.ic_theme
                    ) {
                        ChipFlowRow {
                            ThemeMode.entries.forEach { mode ->
                                SelectableChip(
                                    label = stringResource(mode.labelRes),
                                    selected = state.appearance.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) }
                                )
                            }
                        }
                    }
                    SettingsDivider()
                    SettingsSliderRow(
                        title = stringResource(R.string.settings_font_scale),
                        iconRes = R.drawable.ic_text_size,
                        value = state.appearance.readerFontScale,
                        valueLabel = ArabicText.digits(
                            (state.appearance.readerFontScale * 100).toInt()
                        ),
                        valueRange = AppearanceSettings.MIN_READER_SCALE..
                            AppearanceSettings.MAX_READER_SCALE,
                        steps = 9,
                        onValueChange = viewModel::setReaderFontScale
                    )
                    SettingsFootnote(text = stringResource(R.string.settings_font_scale_desc))
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_hijri),
                        subtitle = stringResource(R.string.settings_hijri_desc),
                        iconRes = R.drawable.ic_calendar,
                        checked = state.appearance.showHijri,
                        onCheckedChange = viewModel::setShowHijri
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // ----------------------------------------------------- tasbeeh
            item(key = "tasbeeh") {
                SettingsSection(title = stringResource(R.string.settings_section_tasbeeh)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_haptics),
                        subtitle = stringResource(R.string.settings_haptics_desc),
                        iconRes = R.drawable.ic_vibration,
                        checked = state.tasbeeh.haptic,
                        onCheckedChange = viewModel::setTasbeehHaptic
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // --------------------------------------------- privacy / about
            item(key = "legal") {
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    SettingsNavRow(
                        title = stringResource(R.string.settings_privacy_row),
                        subtitle = stringResource(R.string.settings_privacy_desc),
                        iconRes = R.drawable.ic_privacy_lock,
                        onClick = onOpenPrivacy
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_about_row),
                        subtitle = stringResource(R.string.settings_about_desc),
                        iconRes = R.drawable.ic_info,
                        onClick = onOpenAbout
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.privacy_no_internet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )
            }
        }
    }

    val target = timeTarget
    if (target != null) {
        val initial = when (target) {
            TimeTarget.MORNING -> state.morning.minuteOfDay
            TimeTarget.EVENING -> state.evening.minuteOfDay
            TimeTarget.FRIDAY -> state.friday.minuteOfDay
            TimeTarget.QUIET_START -> state.quietHours.startMinute
            TimeTarget.QUIET_END -> state.quietHours.endMinute
        }
        val title = when (target) {
            TimeTarget.QUIET_START -> stringResource(R.string.settings_quiet_from)
            TimeTarget.QUIET_END -> stringResource(R.string.settings_quiet_to)
            else -> stringResource(R.string.settings_reminder_time)
        }
        TimePromptDialog(
            title = title,
            initialMinuteOfDay = initial,
            onConfirm = { minuteOfDay ->
                when (target) {
                    TimeTarget.MORNING -> viewModel.setDailyReminder(
                        DailyReminderKind.MORNING,
                        state.morning.enabled,
                        minuteOfDay
                    )
                    TimeTarget.EVENING -> viewModel.setDailyReminder(
                        DailyReminderKind.EVENING,
                        state.evening.enabled,
                        minuteOfDay
                    )
                    TimeTarget.FRIDAY -> viewModel.setDailyReminder(
                        DailyReminderKind.FRIDAY,
                        state.friday.enabled,
                        minuteOfDay
                    )
                    TimeTarget.QUIET_START -> viewModel.setQuietHours(
                        minuteOfDay,
                        state.quietHours.endMinute
                    )
                    TimeTarget.QUIET_END -> viewModel.setQuietHours(
                        state.quietHours.startMinute,
                        minuteOfDay
                    )
                }
                timeTarget = null
            },
            onDismiss = { timeTarget = null }
        )
    }

    if (showIntervalDialog) {
        IntervalPromptDialog(
            initialMinutes = state.reminder.intervalMinutes,
            onConfirm = { minutes ->
                viewModel.setReminderInterval(minutes)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false }
        )
    }
}
