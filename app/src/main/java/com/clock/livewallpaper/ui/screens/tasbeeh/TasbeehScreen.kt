package com.clock.livewallpaper.ui.screens.tasbeeh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.core.Vibrations
import com.clock.livewallpaper.data.prefs.TasbeehSettings
import com.clock.livewallpaper.ui.LocalSnackbarHostState
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.ChipFlowRow
import com.clock.livewallpaper.ui.components.ConfirmDialog
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.IconActionButton
import com.clock.livewallpaper.ui.components.NumberPromptDialog
import com.clock.livewallpaper.ui.components.SecondaryButton
import com.clock.livewallpaper.ui.components.SelectableChip
import com.clock.livewallpaper.ui.components.TasbeehRing
import com.clock.livewallpaper.ui.theme.dhikrBodyStyle

/**
 * The tasbeeh: a large ring counter, presets taken from the bundled content, targets of 33 / 100
 * or a custom number, an optional haptic tick and a daily total that resets with the local date.
 */
@Composable
fun TasbeehScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasbeehViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    var showResetDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showSelectionDialog by remember { mutableStateOf(false) }

    val completedMessage = stringResource(R.string.tasbeeh_completed)

    LaunchedEffect(Unit) {
        viewModel.roundCompleted.collect {
            snackbarHostState.showSnackbar(completedMessage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppTopBar(
            title = stringResource(R.string.tasbeeh_title),
            actions = {
                IconActionButton(
                    iconRes = R.drawable.ic_reset,
                    contentDescription = stringResource(R.string.cd_reset),
                    onClick = { showResetDialog = true }
                )
                IconActionButton(
                    iconRes = R.drawable.ic_settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    onClick = onOpenSettings
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            TasbeehRing(
                count = state.count,
                target = state.target,
                label = state.label,
                onTap = {
                    viewModel.increment()
                    if (state.haptic) Vibrations.tick(context)
                }
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.tasbeeh_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            DhikrCard {
                Text(
                    text = stringResource(R.string.tasbeeh_target),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                ChipFlowRow {
                    TasbeehSettings.TARGETS.forEach { target ->
                        SelectableChip(
                            label = ArabicText.digits(target),
                            selected = state.target == target,
                            onClick = { viewModel.setTarget(target) }
                        )
                    }
                    SelectableChip(
                        label = stringResource(R.string.tasbeeh_target_custom),
                        selected = state.isCustomTarget,
                        onClick = { showTargetDialog = true }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            DhikrCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tasbeeh_daily_total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ArabicText.digits(state.dailyTotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.tasbeeh_select_dhikr),
                        onClick = { showSelectionDialog = true },
                        iconRes = R.drawable.ic_edit,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        text = stringResource(R.string.tasbeeh_reset),
                        onClick = { showResetDialog = true },
                        iconRes = R.drawable.ic_reset
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = stringResource(R.string.tasbeeh_reset_title),
            text = stringResource(R.string.tasbeeh_reset_text),
            confirmText = stringResource(R.string.action_reset),
            onConfirm = {
                viewModel.resetRound()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showTargetDialog) {
        NumberPromptDialog(
            title = stringResource(R.string.tasbeeh_custom_target_title),
            hint = stringResource(R.string.tasbeeh_custom_target_hint),
            initialValue = state.customTarget,
            min = TasbeehSettings.MIN_TARGET,
            max = TasbeehSettings.MAX_TARGET,
            errorText = stringResource(
                R.string.tasbeeh_custom_target_error,
                ArabicText.digits(TasbeehSettings.MIN_TARGET),
                ArabicText.digits(TasbeehSettings.MAX_TARGET)
            ),
            onConfirm = { value ->
                viewModel.setTarget(value)
                showTargetDialog = false
            },
            onDismiss = { showTargetDialog = false }
        )
    }

    if (showSelectionDialog) {
        TasbeehSelectionDialog(
            state = state,
            onSelectPreset = { id ->
                viewModel.selectPreset(id)
                showSelectionDialog = false
            },
            onCustomText = { text ->
                viewModel.setCustomText(text)
                showSelectionDialog = false
            },
            onDismiss = { showSelectionDialog = false }
        )
    }
}

@Composable
private fun TasbeehSelectionDialog(
    state: TasbeehUiState,
    onSelectPreset: (Long) -> Unit,
    onCustomText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customText by remember { mutableStateOf(state.customText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.tasbeeh_select_dhikr),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = state.presets, key = { it.id }) { preset ->
                        SelectableChip(
                            label = preset.arabicText,
                            selected = preset.id == state.presetId,
                            onClick = { onSelectPreset(preset.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.tasbeeh_custom_dhikr),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text(stringResource(R.string.tasbeeh_custom_dhikr_hint)) },
                    textStyle = dhikrBodyStyle(fontScale = 0.8f, centered = false),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = customText.isNotBlank(),
                onClick = { onCustomText(customText) }
            ) {
                Text(
                    text = stringResource(R.string.action_save),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
