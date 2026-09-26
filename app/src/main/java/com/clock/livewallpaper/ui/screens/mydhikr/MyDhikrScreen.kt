package com.clock.livewallpaper.ui.screens.mydhikr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.data.local.DhikrEntity
import com.clock.livewallpaper.ui.LocalSnackbarHostState
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.ConfirmDialog
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.EmptyState
import com.clock.livewallpaper.ui.components.Hairline
import com.clock.livewallpaper.ui.components.IconActionButton
import com.clock.livewallpaper.ui.components.StatusPill
import com.clock.livewallpaper.ui.components.TextActionButton
import com.clock.livewallpaper.ui.theme.dhikrBodyStyle

/** "أذكاري": the user's own dhikr, with the switches that decide where each one appears. */
@Composable
fun MyDhikrScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyDhikrViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val deletedMessage = stringResource(R.string.my_dhikr_deleted)

    var pendingDelete by remember { mutableStateOf<DhikrEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.deleted.collect { snackbarHostState.showSnackbar(deletedMessage) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(R.string.my_dhikr_title),
            subtitle = if (state.items.isEmpty()) {
                null
            } else {
                stringResource(
                    R.string.my_dhikr_count_summary,
                    ArabicText.digits(state.items.size)
                )
            },
            actions = {
                IconActionButton(
                    iconRes = R.drawable.ic_add,
                    contentDescription = stringResource(R.string.cd_add),
                    onClick = onAdd,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        if (!state.loading && state.items.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_empty_dhikr,
                title = stringResource(R.string.my_dhikr_empty_title),
                text = stringResource(R.string.my_dhikr_empty_text),
                ctaText = stringResource(R.string.my_dhikr_add),
                onCta = onAdd
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items = state.items, key = { it.id }) { item ->
                MyDhikrCard(
                    item = item,
                    onEdit = { onEdit(item.id) },
                    onDelete = { pendingDelete = item },
                    onEnabledChange = { enabled -> viewModel.setEnabled(item.id, enabled) },
                    onOverlayChange = { include -> viewModel.setIncludeInOverlay(item.id, include) }
                )
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.my_dhikr_delete_title),
            text = stringResource(R.string.my_dhikr_delete_text),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                viewModel.delete(toDelete.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun MyDhikrCard(
    item: DhikrEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onOverlayChange: (Boolean) -> Unit
) {
    val category = DhikrCategory.fromKey(item.category) ?: DhikrCategory.CUSTOM
    DhikrCard(contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                text = stringResource(category.titleRes),
                active = item.isEnabled
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.reading_repeat_count,
                    ArabicText.digits(item.repeatCount)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (item.isDefault) {
                Text(
                    text = stringResource(R.string.my_dhikr_builtin_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = item.arabicText,
            style = dhikrBodyStyle(fontScale = 0.92f, centered = false),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))
        Hairline()

        SwitchLine(
            label = stringResource(R.string.my_dhikr_switch_enabled),
            checked = item.isEnabled,
            onCheckedChange = onEnabledChange
        )
        SwitchLine(
            label = stringResource(R.string.my_dhikr_switch_overlay),
            checked = item.includeInOverlay,
            onCheckedChange = onOverlayChange
        )

        Hairline()
        Spacer(Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextActionButton(
                text = stringResource(R.string.action_edit),
                iconRes = R.drawable.ic_edit,
                onClick = onEdit,
                contentDescription = stringResource(R.string.cd_edit_item)
            )
            if (!item.isDefault) {
                TextActionButton(
                    text = stringResource(R.string.action_delete),
                    iconRes = R.drawable.ic_delete,
                    onClick = onDelete,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SwitchLine(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
