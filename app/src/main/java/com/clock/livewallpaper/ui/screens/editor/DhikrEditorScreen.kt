package com.clock.livewallpaper.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.ui.LocalSnackbarHostState
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.ChipFlowRow
import com.clock.livewallpaper.ui.components.ConfirmDialog
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.InlineNote
import com.clock.livewallpaper.ui.components.NumberStepper
import com.clock.livewallpaper.ui.components.PrimaryButton
import com.clock.livewallpaper.ui.components.SecondaryButton
import com.clock.livewallpaper.ui.components.SectionHeader
import com.clock.livewallpaper.ui.components.SelectableChip
import com.clock.livewallpaper.ui.components.SettingsSwitchRow
import com.clock.livewallpaper.ui.theme.dhikrBodyStyle

/**
 * Create or edit a dhikr.
 *
 * A bundled dhikr can be switched on or off and hidden from the floating card, but its verified
 * Arabic text and repetition count are not editable - the field is disabled and the reason is
 * stated in Arabic on the screen.
 */
@Composable
fun DhikrEditorScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DhikrEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val savedMessage = stringResource(R.string.my_dhikr_saved)
    val deletedMessage = stringResource(R.string.my_dhikr_deleted)

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
                EditorEvent.Deleted -> snackbarHostState.showSnackbar(deletedMessage)
            }
            onDone()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        AppTopBar(
            title = stringResource(
                if (state.isNew) R.string.my_dhikr_add_title else R.string.my_dhikr_edit_title
            ),
            onBack = onDone
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            DhikrCard {
                Text(
                    text = stringResource(R.string.my_dhikr_field_text),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.text,
                    onValueChange = viewModel::onTextChange,
                    enabled = !state.isProtected,
                    isError = state.error == EditorError.BLANK_TEXT,
                    textStyle = dhikrBodyStyle(fontScale = 0.85f, centered = false),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                )
                if (state.error == EditorError.BLANK_TEXT) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.my_dhikr_validation_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.isProtected) {
                    Spacer(Modifier.height(10.dp))
                    InlineNote(text = stringResource(R.string.my_dhikr_builtin_note))
                }
            }

            Spacer(Modifier.height(16.dp))

            DhikrCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                NumberStepper(
                    label = stringResource(R.string.my_dhikr_field_repeat),
                    valueText = ArabicText.digits(state.repeatCount),
                    onDecrease = { viewModel.onRepeatChange(state.repeatCount - 1) },
                    onIncrease = { viewModel.onRepeatChange(state.repeatCount + 1) }
                )
                if (state.error == EditorError.INVALID_REPEAT) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.my_dhikr_validation_repeat,
                            ArabicText.digits(DhikrRepository.MIN_REPEAT),
                            ArabicText.digits(DhikrRepository.MAX_REPEAT)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionHeader(title = stringResource(R.string.my_dhikr_field_category))
            Spacer(Modifier.height(10.dp))
            ChipFlowRow {
                DhikrCategory.ASSIGNABLE.forEach { category ->
                    SelectableChip(
                        label = stringResource(category.titleRes),
                        selected = state.category == category,
                        enabled = !state.isProtected,
                        onClick = { viewModel.onCategoryChange(category) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            DhikrCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.my_dhikr_switch_enabled),
                    checked = state.isEnabled,
                    onCheckedChange = viewModel::onEnabledChange
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.my_dhikr_switch_overlay),
                    checked = state.includeInOverlay,
                    onCheckedChange = viewModel::onOverlayChange
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    modifier = Modifier.weight(1f)
                )
                if (state.canDelete) {
                    SecondaryButton(
                        text = stringResource(R.string.action_delete),
                        onClick = { showDeleteDialog = true },
                        iconRes = R.drawable.ic_delete
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.my_dhikr_delete_title),
            text = stringResource(R.string.my_dhikr_delete_text),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
