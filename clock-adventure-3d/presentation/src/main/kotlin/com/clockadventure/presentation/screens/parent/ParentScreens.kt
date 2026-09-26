package com.clockadventure.presentation.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockadventure.presentation.R
import com.clockadventure.presentation.components.AdventureBackground
import com.clockadventure.presentation.components.ArcadeButton
import com.clockadventure.presentation.components.ConfirmDialog
import com.clockadventure.presentation.components.GlassCard
import com.clockadventure.presentation.components.ScreenHeader
import com.clockadventure.presentation.components.ShieldIcon
import com.clockadventure.presentation.components.StatBar
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors
import com.clockadventure.presentation.util.DateText

/**
 * The grown-up gate. A multiplication is asked before anything else is shown, so a child cannot
 * reach the parent dashboard (which contains statistics and a reset button) by accident.
 */
@Composable
fun ParentGateRoute(
    onBack: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentGateViewModel = hiltViewModel()
) {
    val question by viewModel.question.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(title = stringResource(R.string.parent_gate_title), onBack = onBack)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.gapLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ShieldIcon(tint = appColors.accent, modifier = Modifier.size(Dimens.minTouchTarget))
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                    Text(
                        text = stringResource(R.string.parent_gate_body),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    if (failed) {
                        Text(
                            text = stringResource(R.string.parent_gate_wrong),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.Coral,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                    Text(
                        text = question.text(com.clockadventure.domain.model.AppLanguage.ENGLISH),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)) {
                        question.options.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                            ) {
                                row.forEach { option ->
                                    ArcadeButton(
                                        text = option.toString(),
                                        onClick = { viewModel.submit(option, onVerified) },
                                        modifier = Modifier.weight(1f),
                                        topColor = Palette.Ocean,
                                        bottomColor = Palette.DeepBlue
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.gapLarge))
                }
            }
        }
    }
}

@Composable
fun ParentRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ParentScreen(
        state = state,
        onBack = onBack,
        onLimitSelected = viewModel::setDailyLimit,
        onNotificationsChanged = viewModel::setNotifications,
        onReset = viewModel::resetProgress,
        modifier = modifier
    )
}

@Composable
internal fun ParentScreen(
    state: ParentUiState,
    onBack: () -> Unit,
    onLimitSelected: (Int) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = state.progress
    val todaySeconds = state.daily.lastOrNull()?.secondsLearned ?: 0
    val limitMinutes = state.settings.dailyLimitMinutes
    val limits = listOf(0, 15, 20, 30, 45, 60)

    Box(modifier = modifier.fillMaxSize()) {
        AdventureBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.parent_title),
                    subtitle = stringResource(R.string.parent_no_data).takeIf { progress.totalAnswers == 0 },
                    onBack = onBack
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.gapMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
                ) {
                    if (progress.totalAnswers == 0) {
                        Text(
                            text = stringResource(R.string.parent_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.gapLarge)
                        )
                    } else {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.gapMedium)) {
                                Text(
                                    text = stringResource(R.string.parent_today),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${todaySeconds / 60} min • ${progress.totalCorrect} ✓",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                                Text(
                                    text = stringResource(R.string.parent_total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${progress.accuracyPercent}% " +
                                        stringResource(R.string.parent_accuracy) +
                                        " • ${progress.lessonsCompleted} " +
                                        stringResource(R.string.parent_lessons) +
                                        " • ${progress.totalPlaySeconds / 60} min",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.gapMedium)) {
                                Text(
                                    text = stringResource(R.string.parent_daily),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                                val max = state.daily.maxOf { it.secondsLearned }.coerceAtLeast(60)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    state.daily.forEach { stat ->
                                        StatBar(
                                            value = stat.secondsLearned.toFloat(),
                                            label = DateText.shortDay(stat.dateKey),
                                            maxValue = max.toFloat(),
                                            color = appColors.accent,
                                            modifier = Modifier.size(width = 36.dp, height = 100.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (state.strongTopics.isNotEmpty() || state.weakTopics.isNotEmpty()) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(Dimens.gapMedium)) {
                                    if (state.strongTopics.isNotEmpty()) {
                                        Text(
                                            text = stringResource(R.string.parent_strong),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Palette.Mint
                                        )
                                        state.strongTopics.forEach { topic ->
                                            Text(text = "• $topic", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    if (state.weakTopics.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(Dimens.gapSmall))
                                        Text(
                                            text = stringResource(R.string.parent_weak),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Palette.Orange
                                        )
                                        state.weakTopics.forEach { topic ->
                                            Text(text = "• $topic", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Dimens.gapMedium)) {
                            Text(
                                text = stringResource(R.string.parent_limit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Dimens.gapSmall))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                limits.chunked(3).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        row.forEach { minutes ->
                                            val selected = minutes == limitMinutes
                                            ArcadeButton(
                                                text = if (minutes == 0) {
                                                    stringResource(R.string.parent_limit_off)
                                                } else {
                                                    stringResource(R.string.parent_limit_minutes, minutes)
                                                },
                                                onClick = { onLimitSelected(minutes) },
                                                height = 52.dp,
                                                modifier = Modifier.weight(1f),
                                                topColor = if (selected) Palette.Mint else Palette.Silver,
                                                bottomColor = if (selected) Palette.Ocean else Palette.InkSoft
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ReminderCard(
                        enabled = state.settings.notificationsEnabled,
                        onToggle = onNotificationsChanged
                    )

                    ResetCard(onReset = onReset, resetDone = state.resetDone)
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapMedium))
                }
            }
        }
    }
}

/**
 * The daily reminder needs the notification permission on Android 13 and newer. The switch asks for
 * it the moment a grown-up turns the reminder on; if the permission is refused the switch simply
 * stays off and nothing else in the app changes.
 */
@Composable
private fun ReminderCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var denied by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pending = false
        denied = !granted
        onToggle(granted)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.gapMedium)) {
            Text(
                text = stringResource(R.string.parent_notifications_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.parent_notifications_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            if (denied) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.parent_notifications_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Coral
                )
            }
            Spacer(modifier = Modifier.height(Dimens.gapSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (enabled) R.string.parent_notifications_granted else R.string.parent_notifications_title
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { next ->
                        if (!next) {
                            onToggle(false)
                            return@Switch
                        }
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            onToggle(true)
                        } else {
                            pending = true
                            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ResetCard(onReset: () -> Unit, resetDone: Boolean) {
    var asking = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.gapMedium)) {
            Text(
                text = stringResource(R.string.parent_reset),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimens.gapSmall))
            ArcadeButton(
                text = stringResource(R.string.parent_reset),
                onClick = { asking.value = true },
                topColor = Palette.Coral,
                bottomColor = Palette.DeepBlue,
                modifier = Modifier.fillMaxWidth()
            )
            if (resetDone) {
                Spacer(modifier = Modifier.height(Dimens.gapSmall))
                Text(
                    text = stringResource(R.string.parent_reset_done),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Mint
                )
            }
        }
    }
    if (asking.value) {
        ConfirmDialog(
            title = stringResource(R.string.parent_reset_title),
            message = stringResource(R.string.parent_reset_body),
            confirmText = stringResource(R.string.parent_reset),
            dismissText = stringResource(R.string.common_cancel),
            onConfirm = {
                asking.value = false
                onReset()
            },
            onDismiss = { asking.value = false }
        )
    }
}
