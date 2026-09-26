package com.clockadventure.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.clockadventure.presentation.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.drawPath
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawLine
import androidx.compose.ui.graphics.drawscope.drawRoundRect
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

/**
 * Every icon of the app is drawn with Canvas.
 *
 * That keeps the APK tiny (no PNGs, no vector XML parsing on the main thread) and lets the icons
 * use exactly the same gradients and colours as the rest of the game.
 */

fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color,
    innerRatio: Float = 0.46f,
    points: Int = 5
) {
    val path = Path()
    val steps = points * 2
    for (i in 0..steps) {
        val index = i % steps
        val angle = (Math.PI / points) * index - Math.PI / 2.0
        val r = if (index % 2 == 0) radius else radius * innerRatio
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}

private fun DrawScope.drawLineIcon(
    color: Color,
    start: Offset,
    end: Offset,
    width: Float = 0f
) {
    val strokeWidth = if (width <= 0f) size.minDimension * 0.13f else width
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

@Composable
fun StarIcon(modifier: Modifier = Modifier, tint: Color = Palette.Gold, filled: Boolean = true) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.46f
        val center = Offset(size.width / 2f, size.height / 2f)
        if (filled) {
            drawStar(center, radius, tint)
        } else {
            drawCircle(color = tint.copy(alpha = 0.25f), radius = radius, center = center)
        }
    }
}

@Composable
fun CoinIcon(modifier: Modifier = Modifier, tint: Color = Palette.Gold) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.46f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tint.copy(alpha = 0.95f), tint, Color(0xFFE39B00)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = radius * 0.62f, center = center)
        drawStar(center = center, radius = radius * 0.52f, color = Color(0xFFE39B00), innerRatio = 0.44f)
    }
}

@Composable
fun CheckIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF3ED9A3)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLineIcon(tint, Offset(w * 0.18f, h * 0.55f), Offset(w * 0.42f, h * 0.76f))
        drawLineIcon(tint, Offset(w * 0.42f, h * 0.76f), Offset(w * 0.84f, h * 0.26f))
    }
}

@Composable
fun CrossIcon(modifier: Modifier = Modifier, tint: Color = Palette.Coral) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLineIcon(tint, Offset(w * 0.22f, h * 0.24f), Offset(w * 0.78f, h * 0.76f))
        drawLineIcon(tint, Offset(w * 0.78f, h * 0.24f), Offset(w * 0.22f, h * 0.76f))
    }
}

@Composable
fun HomeIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.92f, h * 0.5f)
            lineTo(w * 0.78f, h * 0.5f)
            lineTo(w * 0.78f, h * 0.86f)
            lineTo(w * 0.22f, h * 0.86f)
            lineTo(w * 0.22f, h * 0.5f)
            lineTo(w * 0.08f, h * 0.5f)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
fun TrophyIcon(modifier: Modifier = Modifier, tint: Color = Palette.Gold) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.24f, h * 0.12f),
            size = Size(w * 0.52f, h * 0.46f),
            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.24f, h * 0.24f),
            end = Offset(w * 0.10f, h * 0.34f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.76f, h * 0.24f),
            end = Offset(w * 0.90f, h * 0.34f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.40f, h * 0.58f),
            size = Size(w * 0.20f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.26f, h * 0.80f),
            size = Size(w * 0.48f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
    }
}

@Composable
fun GearIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.30f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = tint, radius = radius, center = center)
        drawCircle(color = tint, radius = radius * 1.55f, center = center, style = Stroke(width = radius * 0.34f))
        for (i in 0 until 8) {
            val angle = (PI * 2.0 / 8.0) * i
            val distance = radius * 1.55f
            drawCircle(
                color = tint,
                radius = radius * 0.20f,
                center = Offset(
                    x = center.x + (distance * cos(angle)).toFloat(),
                    y = center.y + (distance * sin(angle)).toFloat()
                )
            )
        }
    }
}

@Composable
fun LockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.28f, h * 0.18f),
            size = Size(w * 0.44f, h * 0.40f),
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.48f),
            size = Size(w * 0.64f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
        )
    }
}

@Composable
fun ClockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.44f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = tint, radius = radius, center = center)
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - radius * 0.62f),
            strokeWidth = radius * 0.14f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + radius * 0.52f, center.y + radius * 0.18f),
            strokeWidth = radius * 0.12f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun BookIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.10f, h * 0.16f),
            size = Size(w * 0.36f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.52f, h * 0.16f),
            size = Size(w * 0.36f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
    }
}

@Composable
fun GamePadIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.06f, h * 0.28f),
            size = Size(w * 0.88f, h * 0.46f),
            cornerRadius = CornerRadius(h * 0.22f, h * 0.22f)
        )
        drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.28f, h * 0.51f))
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.70f, h * 0.42f))
        drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.78f, h * 0.60f))
    }
}

@Composable
fun ChartIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bars = listOf(0.40f, 0.70f, 0.55f, 0.95f)
        bars.forEachIndexed { index, fraction ->
            val barWidth = w * 0.16f
            val left = w * 0.10f + index * (barWidth + w * 0.08f)
            drawRoundRect(
                color = tint,
                topLeft = Offset(left, h * (1f - fraction * 0.86f)),
                size = Size(barWidth, h * fraction * 0.86f),
                cornerRadius = CornerRadius(barWidth * 0.4f, barWidth * 0.4f)
            )
        }
    }
}

@Composable
fun ShieldIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.10f)
            lineTo(w * 0.88f, h * 0.26f)
            lineTo(w * 0.88f, h * 0.52f)
            lineTo(w * 0.5f, h * 0.90f)
            lineTo(w * 0.12f, h * 0.52f)
            lineTo(w * 0.12f, h * 0.26f)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
fun ArrowBackIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLineIcon(tint, Offset(w * 0.74f, h * 0.22f), Offset(w * 0.30f, h * 0.5f))
        drawLineIcon(tint, Offset(w * 0.30f, h * 0.5f), Offset(w * 0.74f, h * 0.78f))
    }
}

@Composable
fun MusicIcon(modifier: Modifier = Modifier, tint: Color = Color.White, muted: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLineIcon(tint, Offset(w * 0.26f, h * 0.72f), Offset(w * 0.26f, h * 0.24f), w * 0.10f)
        drawLineIcon(tint, Offset(w * 0.74f, h * 0.66f), Offset(w * 0.74f, h * 0.18f), w * 0.10f)
        drawLineIcon(tint, Offset(w * 0.26f, h * 0.24f), Offset(w * 0.74f, h * 0.18f), w * 0.10f)
        drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.26f, h * 0.74f))
        drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.74f, h * 0.68f))
        if (muted) {
            drawLineIcon(Palette.Coral, Offset(w * 0.10f, h * 0.86f), Offset(w * 0.90f, h * 0.14f), w * 0.10f)
        }
    }
}

@Composable
fun SpeakerIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.38f)
            lineTo(w * 0.58f, h * 0.16f)
            lineTo(w * 0.58f, h * 0.84f)
            lineTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.12f, h * 0.62f)
            close()
        }
        drawPath(path, tint)
        drawArc(
            color = tint,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.60f, h * 0.28f),
            size = Size(w * 0.30f, h * 0.44f),
            style = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun RefreshIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.34f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            color = tint,
            startAngle = 40f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = radius * 0.30f, cap = StrokeCap.Round)
        )
        drawCircle(color = tint, radius = radius * 0.28f, center = Offset(center.x + radius * 0.86f, center.y - radius * 0.10f))
    }
}

@Composable
fun PlayIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.26f, h * 0.16f)
            lineTo(w * 0.86f, h * 0.5f)
            lineTo(w * 0.26f, h * 0.84f)
            close()
        }
        drawPath(path, tint)
    }
}

@Composable
fun BulbIcon(modifier: Modifier = Modifier, tint: Color = Palette.SunYellow) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = tint, radius = w * 0.30f, center = Offset(w * 0.5f, h * 0.40f))
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.36f, h * 0.62f),
            size = Size(w * 0.28f, h * 0.22f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
    }
}

/** Rounded coin badge used in the HUD. */
@Composable
fun CoinBadge(count: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        CoinIcon(modifier = Modifier.size(22.dp))
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
        androidx.compose.material3.Text(
            text = count.toString(),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

private val Size.minDimension: Float
    get() = kotlin.math.min(width, height)
