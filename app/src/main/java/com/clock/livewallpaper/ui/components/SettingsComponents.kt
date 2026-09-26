package com.clock.livewallpaper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.R

/** A titled group of settings rows inside one card. */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = title)
        Spacer(Modifier.height(10.dp))
        DhikrCard(contentPadding = PaddingValues(vertical = 4.dp), content = content)
    }
}

@Composable
private fun RowShell(
    iconRes: Int?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role = Role.Button,
    trailing: @Composable (() -> Unit)? = null
) {
    val base = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 60.dp)
    val clickable = if (onClick != null) {
        base.clickable(role = role, onClickLabel = onClickLabel, onClick = onClick)
    } else {
        base
    }
    Row(
        modifier = clickable.padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int? = null,
    enabled: Boolean = true
) {
    RowShell(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        role = Role.Switch,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    )
}

@Composable
fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int? = null
) {
    RowShell(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}

@Composable
fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int? = null
) {
    RowShell(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = onClick,
        trailing = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

/** Static row: a label and a value that is read, not tapped (version, build, counts). */
@Composable
fun SettingsInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null
) {
    RowShell(
        iconRes = iconRes,
        title = title,
        subtitle = null,
        modifier = modifier,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/** Row that hosts a group of chips underneath its title (theme, position, style...). */
@Composable
fun SettingsChoiceRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        RowShell(iconRes = iconRes, title = title, subtitle = subtitle)
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp)) {
            content()
        }
    }
}

/** Labelled slider row with the value rendered in Arabic-Indic digits by the caller. */
@Composable
fun SettingsSliderRow(
    title: String,
    value: Float,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    iconRes: Int? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(14.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

/** Divider used between rows of the same card. */
@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    Hairline(modifier = modifier.padding(horizontal = 18.dp))
}

/**
 * Chip strip that wraps onto extra lines when the chips do not fit - hand-rolled so it behaves
 * identically at every font scale and mirrors correctly in RTL (first child starts at the right).
 */
@Composable
fun ChipFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    verticalSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(itemConstraints) }

        val rows = mutableListOf<MutableList<Placeable>>()
        var current = mutableListOf<Placeable>()
        var currentWidth = 0
        placeables.forEach { placeable ->
            val extra = if (current.isEmpty()) placeable.width else placeable.width + hGap
            if (current.isNotEmpty() && currentWidth + extra > maxWidth) {
                rows += current
                current = mutableListOf()
                currentWidth = 0
                currentWidth += placeable.width
            } else {
                currentWidth += extra
            }
            current += placeable
        }
        if (current.isNotEmpty()) rows += current

        val totalHeight = rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 } +
            vGap * (rows.size - 1).coerceAtLeast(0)
        val width = if (constraints.hasBoundedWidth) maxWidth else {
            rows.maxOfOrNull { row -> row.sumOf { it.width } + hGap * (row.size - 1) } ?: 0
        }

        layout(width, totalHeight.coerceAtLeast(0)) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { placeable ->
                    // placeRelative mirrors the whole strip in RTL.
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGap
                }
                y += (row.maxOfOrNull { it.height } ?: 0) + vGap
            }
        }
    }
}

/** Card footer note in the muted ink. */
@Composable
fun SettingsFootnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}

/** Short explanatory paragraph shown under a screen title. */
@Composable
fun SettingsIntro(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}
