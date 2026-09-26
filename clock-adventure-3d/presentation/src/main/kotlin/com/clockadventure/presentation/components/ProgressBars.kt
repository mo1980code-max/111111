package com.clockadventure.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clockadventure.presentation.theme.Palette
import kotlin.math.max
import androidx.compose.ui.graphics.drawscope.drawRoundRect

/** Three stars; the earned ones are filled. */
@Composable
fun StarRow(
    stars: Int,
    modifier: Modifier = Modifier,
    max: Int = 3,
    size: Dp = 26.dp,
    filledColor: Color = Palette.Gold
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { index ->
            StarIcon(
                modifier = Modifier.size(size),
                tint = if (index < stars) filledColor else Palette.Locked.copy(alpha = 0.55f),
                filled = index < stars
            )
        }
    }
}

/**
 * The xp / progress bar of the app: a rounded track with a gradient fill and a glossy top half,
 * animated so it never jumps.
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    trackColor: Color = Color.White.copy(alpha = 0.55f),
    fillTop: Color = Palette.Mint,
    fillBottom: Color = Color(0xFF1E9E6E)
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progress_bar"
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = max(animated, 0.02f))
                .fillMaxHeight()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
        ) { }
    }
}

/** Small vertical bar used by the parent dashboard chart. */
@Composable
fun StatBar(
    value: Float,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Palette.Ocean,
    maxValue: Float = 1f
) {
    val fraction = if (maxValue <= 0f) 0f else (value / maxValue).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 500),
        label = "stat_bar"
    )
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.6f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height((90 * animated.coerceAtLeast(0.03f)).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.85f), color)))
            ) { }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Answer progress "3 / 8" as a row of little pills. */
@Composable
fun StepDots(total: Int, current: Int, modifier: Modifier = Modifier, color: Color = Palette.CandyPink) {
    Canvas(modifier = modifier.height(12.dp).fillMaxWidth()) {
        val count = max(total, 1)
        val spacing = 10f
        val width = (size.width - spacing * (count - 1)) / count
        for (i in 0 until count) {
            val left = i * (width + spacing)
            drawRoundRect(
                color = if (i < current) color else color.copy(alpha = 0.25f),
                topLeft = Offset(left, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
        }
    }
}
