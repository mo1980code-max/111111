package com.clock.livewallpaper.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
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
import com.clock.livewallpaper.core.DayPart
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.overlay.OverlayPermission
import com.clock.livewallpaper.ui.LocalSnackbarHostState
import com.clock.livewallpaper.ui.components.DailyDhikrCard
import com.clock.livewallpaper.ui.components.HeroCard
import com.clock.livewallpaper.ui.components.HomeHeader
import com.clock.livewallpaper.ui.components.IntervalPromptDialog
import com.clock.livewallpaper.ui.components.PermissionCard
import com.clock.livewallpaper.ui.components.QuickActionCard
import com.clock.livewallpaper.ui.components.ReminderCard
import com.clock.livewallpaper.ui.components.SectionHeader
import com.clock.livewallpaper.ui.util.Sharing
import kotlinx.coroutines.launch

/**
 * Home: greeting and date, the dynamic morning / evening hero, the reminder control, the quick
 * actions and today's dhikr. Everything on this screen is produced locally.
 */
@Composable
fun HomeScreen(
    onOpenReading: (DhikrCategory) -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenTasbeeh: () -> Unit,
    onOpenMyDhikr: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    var overlayGranted by remember { mutableStateOf(OverlayPermission.canDraw(context)) }
    var showIntervalDialog by remember { mutableStateOf(false) }

    // Date, dhikr of the day and the overlay permission are re-read every time Home comes back.
    LifecycleResumeEffect(Unit) {
        overlayGranted = OverlayPermission.canDraw(context)
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val copiedMessage = stringResource(R.string.copied_to_clipboard)
    val clipLabel = stringResource(R.string.app_name)
    val chooserTitle = stringResource(R.string.share_chooser_title)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            HomeHeader(
                gregorian = state.gregorianDate,
                hijri = state.hijriDate,
                onSettings = onOpenSettings
            )
        }

        item(key = "hero") {
            HeroCard(
                dayPart = state.dayPart,
                onStart = {
                    onOpenReading(
                        if (state.dayPart == DayPart.MORNING) {
                            DhikrCategory.MORNING
                        } else {
                            DhikrCategory.EVENING
                        }
                    )
                }
            )
        }

        if (state.overlayEnabled && !overlayGranted) {
            item(key = "overlay_permission") {
                PermissionCard(
                    iconRes = R.drawable.ic_card,
                    title = stringResource(R.string.overlay_permission_title),
                    text = stringResource(R.string.overlay_permission_text),
                    note = stringResource(R.string.overlay_permission_privacy),
                    ctaText = stringResource(R.string.overlay_permission_cta),
                    onCta = {
                        runCatching { context.startActivity(OverlayPermission.settingsIntent(context)) }
                    },
                    onDismiss = null
                )
            }
        }

        item(key = "reminder") {
            ReminderCard(
                enabled = state.reminderEnabled,
                intervalMinutes = state.intervalMinutes,
                onToggle = viewModel::setReminderEnabled,
                onIntervalSelected = viewModel::setInterval,
                onCustomInterval = { showIntervalDialog = true }
            )
        }

        item(key = "quick_actions") {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(title = stringResource(R.string.home_quick_actions))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        iconRes = R.drawable.ic_sun,
                        label = stringResource(R.string.widget_morning),
                        onClick = { onOpenReading(DhikrCategory.MORNING) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        iconRes = R.drawable.ic_moon,
                        label = stringResource(R.string.widget_evening),
                        onClick = { onOpenReading(DhikrCategory.EVENING) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        iconRes = R.drawable.ic_nav_tasbeeh,
                        label = stringResource(R.string.widget_tasbeeh),
                        onClick = onOpenTasbeeh,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        iconRes = R.drawable.ic_nav_my_dhikr,
                        label = stringResource(R.string.nav_my_dhikr),
                        onClick = onOpenMyDhikr,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (state.dailyDhikrText.isNotBlank()) {
            item(key = "daily_dhikr") {
                DailyDhikrCard(
                    text = state.dailyDhikrText,
                    onCopy = {
                        val showConfirmation = Sharing.copy(
                            context = context,
                            text = state.dailyDhikrText,
                            label = clipLabel
                        )
                        if (showConfirmation) {
                            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                        }
                    },
                    onShare = {
                        Sharing.share(
                            context = context,
                            text = state.dailyDhikrText,
                            chooserTitle = chooserTitle
                        )
                    }
                )
            }
        }

        item(key = "browse_all") {
            QuickActionCard(
                iconRes = R.drawable.ic_nav_adhkar,
                label = stringResource(R.string.adhkar_title),
                onClick = onOpenAdhkar,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showIntervalDialog) {
        IntervalPromptDialog(
            initialMinutes = state.intervalMinutes,
            onConfirm = { minutes ->
                viewModel.setInterval(minutes)
                showIntervalDialog = false
            },
            onDismiss = { showIntervalDialog = false }
        )
    }
}
