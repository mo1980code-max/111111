package com.clock.livewallpaper.ui.screens.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.core.Vibrations
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.ui.LocalSnackbarHostState
import com.clock.livewallpaper.ui.components.AppTopBar
import com.clock.livewallpaper.ui.components.DhikrCard
import com.clock.livewallpaper.ui.components.DhikrProgress
import com.clock.livewallpaper.ui.components.EmptyState
import com.clock.livewallpaper.ui.components.Hairline
import com.clock.livewallpaper.ui.components.IconActionButton
import com.clock.livewallpaper.ui.components.PrimaryButton
import com.clock.livewallpaper.ui.components.SecondaryButton
import com.clock.livewallpaper.ui.components.StatusPill
import com.clock.livewallpaper.ui.components.TextActionButton
import com.clock.livewallpaper.ui.theme.LocalDhikrColors
import com.clock.livewallpaper.ui.theme.dhikrBodyStyle
import com.clock.livewallpaper.ui.util.Sharing
import kotlinx.coroutines.launch

/**
 * Guided reading: the verified Arabic text, a repetition counter per dhikr and an overall
 * progress rule. The text itself is never reflowed, shortened or restyled - only the UI around
 * it adapts to the chosen reading size.
 */
@Composable
fun ReadingScreen(
    onBack: () -> Unit,
    onAddDhikr: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val clipLabel = stringResource(R.string.app_name)
    val copiedMessage = stringResource(R.string.copied_to_clipboard)
    val chooserTitle = stringResource(R.string.share_chooser_title)

    // Follow the reader: when a dhikr is finished the next one is brought into view.
    LaunchedEffect(state.currentIndex) {
        val index = state.currentIndex
        if (index > 0) {
            runCatching { listState.animateScrollToItem(index + 1) }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(state.category.titleRes),
            onBack = onBack,
            actions = {
                IconActionButton(
                    iconRes = R.drawable.ic_reset,
                    contentDescription = stringResource(R.string.cd_reset),
                    onClick = viewModel::restart
                )
            }
        )

        if (!state.loading && state.items.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_empty_dhikr,
                title = stringResource(R.string.category_empty_title),
                text = stringResource(R.string.category_empty_text),
                ctaText = if (state.category == DhikrCategory.CUSTOM) {
                    stringResource(R.string.my_dhikr_add)
                } else {
                    null
                },
                onCta = if (state.category == DhikrCategory.CUSTOM) onAddDhikr else null
            )
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "progress") {
                ProgressHeader(
                    completed = state.completedCount,
                    total = state.total,
                    progress = state.progress
                )
            }

            itemsIndexed(
                items = state.items,
                key = { _, item -> item.entity.id }
            ) { index, item ->
                ReadingCard(
                    index = index,
                    item = item,
                    fontScale = state.readerFontScale,
                    onCount = {
                        if (!item.completed) {
                            viewModel.count(item)
                            if (state.haptic) Vibrations.tick(context)
                        }
                    },
                    onComplete = { viewModel.complete(item) },
                    onCopy = {
                        val confirm = Sharing.copy(context, item.entity.arabicText, clipLabel)
                        if (confirm) {
                            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                        }
                    },
                    onShare = {
                        Sharing.share(context, item.entity.arabicText, chooserTitle)
                    }
                )
            }

            if (state.allDone) {
                item(key = "all_done") {
                    DhikrCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.reading_all_done),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        SecondaryButton(
                            text = stringResource(R.string.reading_restart),
                            onClick = viewModel::restart,
                            iconRes = R.drawable.ic_reset,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(completed: Int, total: Int, progress: Float) {
    val description = stringResource(
        R.string.cd_progress,
        ArabicText.digits(completed),
        ArabicText.digits(total)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { contentDescription = description }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reading_progress, ArabicText.digits(completed), ArabicText.digits(total)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        DhikrProgress(progress = progress)
    }
}

@Composable
private fun ReadingCard(
    index: Int,
    item: ReadingItem,
    fontScale: Float,
    onCount: () -> Unit,
    onComplete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val extras = LocalDhikrColors.current
    var virtueExpanded by remember { mutableStateOf(false) }
    val container by animateColorAsState(
        targetValue = if (item.completed) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(220),
        label = "readingCardContainer"
    )

    DhikrCard(
        containerColor = container,
        onClick = if (item.completed) null else onCount,
        onClickLabel = stringResource(R.string.reading_tap_to_count),
        contentPadding = PaddingValues(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ArabicText.digits(index + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(
                    R.string.reading_repeat_count,
                    ArabicText.digits(item.target)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (item.completed) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.reading_item_done),
                    tint = extras.positive,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                StatusPill(
                    text = stringResource(
                        R.string.reading_remaining,
                        ArabicText.digits(item.remaining)
                    ),
                    active = true
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = item.entity.arabicText,
            style = dhikrBodyStyle(fontScale = fontScale),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        val virtue = item.entity.virtue
        if (!virtue.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            TextActionButton(
                text = stringResource(R.string.reading_virtue),
                iconRes = R.drawable.ic_info,
                onClick = { virtueExpanded = !virtueExpanded },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedVisibility(visible = virtueExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp)
                ) {
                    Text(
                        text = virtue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val sourceLabel = item.entity.sourceName?.takeIf { it.isNotBlank() }
            ?: item.entity.sourceReference?.takeIf { it.isNotBlank() }
        if (sourceLabel != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(R.drawable.ic_source),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.reading_source_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.entity.isDefault) {
                        Text(
                            text = stringResource(R.string.reading_source_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(10.dp))

        if (item.completed) {
            Text(
                text = stringResource(R.string.reading_item_done),
                style = MaterialTheme.typography.labelMedium,
                color = extras.positive
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.reading_tap_to_count),
                onClick = onCount,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = stringResource(R.string.reading_item_done),
                onClick = onComplete,
                iconRes = R.drawable.ic_check,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextActionButton(
                text = stringResource(R.string.action_copy),
                iconRes = R.drawable.ic_copy,
                onClick = onCopy,
                contentDescription = stringResource(R.string.cd_copy),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextActionButton(
                text = stringResource(R.string.action_share),
                iconRes = R.drawable.ic_share,
                onClick = onShare,
                contentDescription = stringResource(R.string.cd_share),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
