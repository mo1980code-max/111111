package com.clockadventure.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clockadventure.presentation.R
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors

/** Coins, stars and the level badge, shown on top of most screens. */
@Composable
fun RewardHud(
    coins: Int,
    stars: Int,
    level: Int,
    levelProgress: Float,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.gapSmall)
    ) {
        HudChip {
            CoinIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = coins.toString(), style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        HudChip {
            StarIcon(modifier = Modifier.size(20.dp), tint = Palette.Gold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stars.toString(), style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Box(modifier = Modifier.weight(1f)) {
            LevelBadge(level = level, progress = levelProgress)
        }
        if (onSettingsClick != null) {
            RoundIconButton(onClick = onSettingsClick, size = 46.dp) {
                GearIcon(modifier = Modifier.size(22.dp), tint = appColors.accentDark)
            }
        }
    }
}

@Composable
private fun HudChip(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .height(46.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6A5ACD), Color(0xFF3B2F8C))
                )
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun LevelBadge(level: Int, progress: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.common_level, level),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        ProgressBar(
            progress = progress,
            height = 12.dp,
            trackColor = Color.White.copy(alpha = 0.45f),
            fillTop = Palette.SunYellow,
            fillBottom = Palette.Orange
        )
    }
}

/** Title row with a back button, used by every secondary screen. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.gapMedium)
    ) {
        if (onBack != null) {
            RoundIconButton(onClick = onBack, size = 52.dp) {
                ArrowBackIcon(modifier = Modifier.size(26.dp), tint = appColors.accentDark)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}
