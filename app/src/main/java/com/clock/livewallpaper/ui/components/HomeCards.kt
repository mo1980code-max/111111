package com.clock.livewallpaper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.core.DayPart
import com.clock.livewallpaper.data.prefs.ReminderSettings
import com.clock.livewallpaper.ui.theme.HeroPalette
import com.clock.livewallpaper.ui.theme.LocalDhikrColors
import com.clock.livewallpaper.ui.theme.dhikrBodyStyle

/**
 * The home hero. Artwork is drawn with Compose - an abstract sunrise (stacked arcs rising over a
 * horizon) in the morning, an abstract night (a crescent arc and a few still points) in the
 * evening. No emoji, no clip-art, no bitmap.
 */
@Composable
fun HeroCard(
    dayPart: DayPart,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = LocalDhikrColors.current
    val palette: HeroPalette =
        if (dayPart == DayPart.MORNING) extras.morningHero else extras.eveningHero
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(palette.top, palette.bottom)))
            .border(1.dp, palette.accent.copy(alpha = 0.22f), shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (dayPart == DayPart.MORNING) {
                val cx = w * 0.20f
                val cy = h * 0.96f
                listOf(0.34f, 0.50f, 0.66f).forEachIndexed { index, factor ->
                    val radius = h * factor
                    drawArc(
                        color = palette.accent.copy(alpha = 0.30f - index * 0.07f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            } else {
                val radius = h * 0.42f
                val cx = w * 0.20f
                val cy = h * 0.34f
                drawArc(
                    color = palette.accent.copy(alpha = 0.55f),
                    startAngle = 40f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                listOf(
                    Offset(w * 0.40f, h * 0.22f),
                    Offset(w * 0.32f, h * 0.62f),
                    Offset(w * 0.06f, h * 0.70f)
                ).forEach { point ->
                    drawCircle(
                        color = palette.accent.copy(alpha = 0.45f),
                        radius = 2.2.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(
                    if (dayPart == DayPart.MORNING) {
                        R.string.home_hero_morning_title
                    } else {
                        R.string.home_hero_evening_title
                    }
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (dayPart == DayPart.MORNING) {
                        R.string.home_hero_morning_subtitle
                    } else {
                        R.string.home_hero_evening_subtitle
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = stringResource(R.string.home_hero_cta),
                onClick = onStart,
                container = palette.ink,
                content = palette.top
            )
        }
    }
}

/** Home quick action tile. */
@Composable
fun QuickActionCard(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    iconContainer: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .border(1.dp, LocalDhikrColors.current.hairline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(iconRes = iconRes, container = iconContainer)
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The reminder card on home: switch, live status, current interval and the quick intervals.
 * Every control writes straight through to the persisted reminder settings.
 */
@Composable
fun ReminderCard(
    enabled: Boolean,
    intervalMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onCustomInterval: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    DhikrCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconRes = R.drawable.ic_bell)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_reminder_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.home_reminder_every,
                        ArabicText.duration(context, intervalMinutes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        Spacer(Modifier.height(14.dp))
        StatusPill(
            text = stringResource(
                if (enabled) R.string.home_reminder_active else R.string.home_reminder_paused
            ),
            active = enabled
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReminderSettings.QUICK_MINUTES.forEach { minutes ->
                SelectableChip(
                    label = ArabicText.duration(context, minutes),
                    selected = enabled && intervalMinutes == minutes,
                    onClick = { onIntervalSelected(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SelectableChip(
            label = stringResource(R.string.home_reminder_custom),
            selected = enabled && !ReminderSettings.QUICK_MINUTES.contains(intervalMinutes),
            onClick = onCustomInterval,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** "ذكر اليوم" - the dhikr text is the loudest element on the card. */
@Composable
fun DailyDhikrCard(
    text: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    DhikrCard(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LocalDhikrColors.current.gold)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_daily_dhikr),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style = dhikrBodyStyle(),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextActionButton(
                text = stringResource(R.string.action_copy),
                iconRes = R.drawable.ic_copy,
                onClick = onCopy,
                contentDescription = stringResource(R.string.cd_copy)
            )
            TextActionButton(
                text = stringResource(R.string.action_share),
                iconRes = R.drawable.ic_share,
                onClick = onShare,
                contentDescription = stringResource(R.string.cd_share)
            )
        }
    }
}

/** Header row: greeting, dates and the settings entry point. */
@Composable
fun HomeHeader(
    gregorian: String,
    hijri: String?,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = gregorian,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hijri != null) {
                Text(
                    text = hijri,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalDhikrColors.current.gold
                )
            }
        }
        IconActionButton(
            iconRes = R.drawable.ic_settings,
            contentDescription = stringResource(R.string.cd_settings),
            onClick = onSettings
        )
    }
}

/** Small brand lockup used on the onboarding and about screens. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 56.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_brand_mark),
        contentDescription = stringResource(R.string.cd_brand_mark),
        tint = LocalDhikrColors.current.gold,
        modifier = modifier.size(size)
    )
}
