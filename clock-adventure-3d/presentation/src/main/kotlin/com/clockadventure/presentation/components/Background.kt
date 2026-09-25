package com.clockadventure.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.clockadventure.presentation.theme.LocalReduceMotion
import com.clockadventure.presentation.theme.appColors
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.foundation.background

/**
 * The animated sky behind every screen.
 *
 * Everything is drawn with Canvas - no image assets, so the app starts instantly, uses almost no
 * memory and works offline. With "reduce animation" enabled the bubbles simply stand still.
 */
@Composable
fun AdventureBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = appColors
    val reduceMotion = LocalReduceMotion.current

    val transition = rememberInfiniteTransition(label = "adventure_background")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "background_phase"
    )

    val bubbles = remember(colors) { buildBubbles(colors.decorative) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors.skyColors))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            bubbles.forEachIndexed { index, bubble ->
                val wobble = if (reduceMotion) 0f
                else sin((phase * 2.0 * PI + index).toFloat()) * bubble.drift
                val rise = if (reduceMotion) 0f else phase * -height * 0.12f + index * 12f
                val y = ((bubble.y * height + rise) % (height + bubble.radius * 2f) + (height + bubble.radius * 2f)) %
                    (height + bubble.radius * 2f) - bubble.radius
                drawCircle(
                    color = bubble.color,
                    radius = bubble.radius,
                    center = Offset(
                        x = bubble.x * width + wobble,
                        y = y
                    ),
                    alpha = bubble.alpha
                )
            }
        }
        content()
    }
}

private data class Bubble(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val drift: Float,
    val color: Color
)

private fun buildBubbles(colors: List<Color>): List<Bubble> {
    val positions = listOf(
        0.12f to 0.18f, 0.78f to 0.12f, 0.32f to 0.62f, 0.88f to 0.48f,
        0.55f to 0.82f, 0.08f to 0.75f, 0.68f to 0.30f, 0.42f to 0.42f
    )
    return positions.mapIndexed { index, (x, y) ->
        Bubble(
            x = x,
            y = y,
            radius = 26f + (index % 3) * 18f,
            alpha = 0.18f + (index % 3) * 0.06f,
            drift = 14f + index * 3f,
            color = colors[index % colors.size]
        )
    }
}
