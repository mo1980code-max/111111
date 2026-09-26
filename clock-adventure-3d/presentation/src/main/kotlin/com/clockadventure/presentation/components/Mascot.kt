package com.clockadventure.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clockadventure.domain.model.MascotId
import com.clockadventure.presentation.theme.LocalReduceMotion
import com.clockadventure.presentation.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawLine
import androidx.compose.ui.graphics.drawscope.drawOval
import androidx.compose.ui.graphics.drawscope.drawRoundRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawPath

/** How the mascot feels right now; the screens switch it as the child progresses. */
enum class MascotMood { HAPPY, EXCITED, THINKING, SLEEPY }

private data class MascotLook(
    val bodyTop: Color,
    val bodyBottom: Color,
    val belly: Color,
    val cheek: Color,
    val outline: Color
)

private fun lookFor(mascot: MascotId): MascotLook = when (mascot) {
    MascotId.TICKY -> MascotLook(
        bodyTop = Color(0xFF9B7BFF),
        bodyBottom = Color(0xFF6A3CC4),
        belly = Color(0xFFFFF0D6),
        cheek = Color(0xFFFF8FB1),
        outline = Color(0xFF3B1D7A)
    )
    MascotId.ROBO -> MascotLook(
        bodyTop = Color(0xFF5FD7FF),
        bodyBottom = Color(0xFF1E88C7),
        belly = Color(0xFFE8FBFF),
        cheek = Color(0xFF7CE8C0),
        outline = Color(0xFF0B4B73)
    )
    MascotId.KITTY -> MascotLook(
        bodyTop = Color(0xFFFFB27F),
        bodyBottom = Color(0xFFFF7A45),
        belly = Color(0xFFFFF3E2),
        cheek = Color(0xFFFF6FA5),
        outline = Color(0xFF8A3B12)
    )
    MascotId.SPARKY -> MascotLook(
        bodyTop = Color(0xFFFFE27A),
        bodyBottom = Color(0xFFFFB300),
        belly = Color(0xFFFFFBEA),
        cheek = Color(0xFFFF8A3D),
        outline = Color(0xFFB26A00)
    )
}

/**
 * The companion of the app: a cute character drawn with Canvas that floats, blinks and reacts.
 *
 * It is drawn (not an image) so it can be tinted per theme, animated at 60 fps with no decoding
 * cost, and shipped for free inside the APK.
 */
@Composable
fun MascotView(
    mascot: MascotId,
    mood: MascotMood,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 170.dp
) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "mascot")
    val float by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_float"
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot_blink"
    )
    val look = lookFor(mascot)

    Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val w = size.width
            val h = size.height
            val bounce = if (reduceMotion) 0f else (float - 0.5f) * h * 0.06f
            val centerX = w / 2f
            val centerY = h / 2f + bounce
            val bodyRadius = w * 0.30f
            val blinking = if (reduceMotion) false else blink > 0.94f

            when (mascot) {
                MascotId.SPARKY -> drawStarBody(centerX, centerY, bodyRadius * 1.45f, look)
                MascotId.ROBO -> drawRoboBody(centerX, centerY, bodyRadius, look)
                MascotId.KITTY -> drawCatBody(centerX, centerY, bodyRadius, look)
                MascotId.TICKY -> drawOwlBody(centerX, centerY, bodyRadius, look)
            }

            drawFace(
                centerX = centerX,
                centerY = centerY,
                radius = bodyRadius,
                look = look,
                mood = mood,
                blinking = blinking
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOwlBody(
    cx: Float, cy: Float, r: Float, look: MascotLook
) {
    // Ear tufts
    val tuft = Path().apply {
        moveTo(cx - r * 0.78f, cy - r * 0.62f)
        lineTo(cx - r * 0.42f, cy - r * 1.32f)
        lineTo(cx - r * 0.10f, cy - r * 0.86f)
        close()
    }
    drawPath(tuft, look.bodyBottom)
    val tuft2 = Path().apply {
        moveTo(cx + r * 0.78f, cy - r * 0.62f)
        lineTo(cx + r * 0.42f, cy - r * 1.32f)
        lineTo(cx + r * 0.10f, cy - r * 0.86f)
        close()
    }
    drawPath(tuft2, look.bodyBottom)

    // Wings
    drawCircle(color = look.bodyBottom, radius = r * 0.30f, center = Offset(cx - r * 0.86f, cy + r * 0.24f))
    drawCircle(color = look.bodyBottom, radius = r * 0.30f, center = Offset(cx + r * 0.86f, cy + r * 0.24f))

    // Body
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(look.bodyTop, look.bodyBottom),
            startY = cy - r * 1.1f,
            endY = cy + r * 1.1f
        ),
        topLeft = Offset(cx - r, cy - r * 1.06f),
        size = Size(r * 2f, r * 2.1f)
    )
    // Belly
    drawOval(
        color = look.belly,
        topLeft = Offset(cx - r * 0.56f, cy - r * 0.20f),
        size = Size(r * 1.12f, r * 1.10f)
    )
    // Beak
    val beak = Path().apply {
        moveTo(cx, cy + r * 0.10f)
        lineTo(cx - r * 0.16f, cy + r * 0.36f)
        lineTo(cx + r * 0.16f, cy + r * 0.36f)
        close()
    }
    drawPath(beak, Palette.SunYellow)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoboBody(
    cx: Float, cy: Float, r: Float, look: MascotLook
) {
    // Antenna
    drawLine(
        color = look.outline,
        start = Offset(cx, cy - r * 1.02f),
        end = Offset(cx, cy - r * 1.52f),
        strokeWidth = r * 0.10f
    )
    drawCircle(color = Palette.Coral, radius = r * 0.16f, center = Offset(cx, cy - r * 1.60f))

    // Arms
    drawRoundRect(
        color = look.bodyBottom,
        topLeft = Offset(cx - r * 1.34f, cy - r * 0.10f),
        size = Size(r * 0.44f, r * 0.86f),
        cornerRadius = CornerRadius(r * 0.22f, r * 0.22f)
    )
    drawRoundRect(
        color = look.bodyBottom,
        topLeft = Offset(cx + r * 0.90f, cy - r * 0.10f),
        size = Size(r * 0.44f, r * 0.86f),
        cornerRadius = CornerRadius(r * 0.22f, r * 0.22f)
    )

    // Head
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(look.bodyTop, look.bodyBottom),
            startY = cy - r,
            endY = cy + r
        ),
        topLeft = Offset(cx - r * 0.96f, cy - r * 1.02f),
        size = Size(r * 1.92f, r * 1.86f),
        cornerRadius = CornerRadius(r * 0.44f, r * 0.44f)
    )
    // Screen
    drawRoundRect(
        color = look.belly,
        topLeft = Offset(cx - r * 0.66f, cy - r * 0.62f),
        size = Size(r * 1.32f, r * 1.10f),
        cornerRadius = CornerRadius(r * 0.26f, r * 0.26f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCatBody(
    cx: Float, cy: Float, r: Float, look: MascotLook
) {
    // Ears
    val leftEar = Path().apply {
        moveTo(cx - r * 0.92f, cy - r * 0.52f)
        lineTo(cx - r * 0.62f, cy - r * 1.34f)
        lineTo(cx - r * 0.16f, cy - r * 0.78f)
        close()
    }
    drawPath(leftEar, look.bodyBottom)
    val rightEar = Path().apply {
        moveTo(cx + r * 0.92f, cy - r * 0.52f)
        lineTo(cx + r * 0.62f, cy - r * 1.34f)
        lineTo(cx + r * 0.16f, cy - r * 0.78f)
        close()
    }
    drawPath(rightEar, look.bodyBottom)

    // Tail
    drawArc(
        color = look.bodyBottom,
        startAngle = 0f,
        sweepAngle = 200f,
        useCenter = false,
        topLeft = Offset(cx + r * 0.72f, cy + r * 0.10f),
        size = Size(r * 0.90f, r * 0.90f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.20f)
    )

    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(look.bodyTop, look.bodyBottom),
            startY = cy - r,
            endY = cy + r
        ),
        radius = r * 1.02f,
        center = Offset(cx, cy)
    )
    drawOval(
        color = look.belly,
        topLeft = Offset(cx - r * 0.42f, cy + r * 0.16f),
        size = Size(r * 0.84f, r * 0.62f)
    )
    // Whiskers
    val whiskerStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.05f)
    drawLine(color = look.outline, start = Offset(cx - r * 0.32f, cy + r * 0.34f), end = Offset(cx - r * 1.16f, cy + r * 0.26f), strokeWidth = r * 0.05f)
    drawLine(color = look.outline, start = Offset(cx - r * 0.32f, cy + r * 0.44f), end = Offset(cx - r * 1.16f, cy + r * 0.52f), strokeWidth = r * 0.05f)
    drawLine(color = look.outline, start = Offset(cx + r * 0.32f, cy + r * 0.34f), end = Offset(cx + r * 1.16f, cy + r * 0.26f), strokeWidth = r * 0.05f)
    drawLine(color = look.outline, start = Offset(cx + r * 0.32f, cy + r * 0.44f), end = Offset(cx + r * 1.16f, cy + r * 0.52f), strokeWidth = r * 0.05f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarBody(
    cx: Float, cy: Float, r: Float, look: MascotLook
) {
    drawStar(
        center = Offset(cx, cy),
        radius = r,
        color = look.bodyBottom,
        innerRatio = 0.52f,
        points = 5
    )
    drawStar(
        center = Offset(cx, cy),
        radius = r * 0.80f,
        color = look.bodyTop,
        innerRatio = 0.52f,
        points = 5
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFace(
    centerX: Float,
    centerY: Float,
    radius: Float,
    look: MascotLook,
    mood: MascotMood,
    blinking: Boolean
) {
    val eyeOffsetX = radius * 0.38f
    val eyeOffsetY = -radius * 0.22f
    val eyeRadius = radius * 0.24f
    val pupilRadius = radius * 0.12f

    if (blinking || mood == MascotMood.SLEEPY) {
        drawLine(
            color = look.outline,
            start = Offset(centerX - eyeOffsetX - eyeRadius, centerY + eyeOffsetY),
            end = Offset(centerX - eyeOffsetX + eyeRadius, centerY + eyeOffsetY),
            strokeWidth = radius * 0.08f
        )
        drawLine(
            color = look.outline,
            start = Offset(centerX + eyeOffsetX - eyeRadius, centerY + eyeOffsetY),
            end = Offset(centerX + eyeOffsetX + eyeRadius, centerY + eyeOffsetY),
            strokeWidth = radius * 0.08f
        )
    } else {
        // Whites
        drawCircle(color = Color.White, radius = eyeRadius, center = Offset(centerX - eyeOffsetX, centerY + eyeOffsetY))
        drawCircle(color = Color.White, radius = eyeRadius, center = Offset(centerX + eyeOffsetX, centerY + eyeOffsetY))
        // Pupils look a little upwards when the mascot is excited
        val pupilY = centerY + eyeOffsetY - if (mood == MascotMood.EXCITED) radius * 0.04f else 0f
        drawCircle(color = look.outline, radius = pupilRadius, center = Offset(centerX - eyeOffsetX, pupilY))
        drawCircle(color = look.outline, radius = pupilRadius, center = Offset(centerX + eyeOffsetX, pupilY))
        // Sparkles
        drawCircle(
            color = Color.White,
            radius = pupilRadius * 0.42f,
            center = Offset(centerX - eyeOffsetX + pupilRadius * 0.42f, pupilY - pupilRadius * 0.46f)
        )
        drawCircle(
            color = Color.White,
            radius = pupilRadius * 0.42f,
            center = Offset(centerX + eyeOffsetX + pupilRadius * 0.42f, pupilY - pupilRadius * 0.46f)
        )
    }

    // Cheeks
    drawCircle(color = look.cheek.copy(alpha = 0.65f), radius = radius * 0.14f, center = Offset(centerX - radius * 0.70f, centerY + radius * 0.16f))
    drawCircle(color = look.cheek.copy(alpha = 0.65f), radius = radius * 0.14f, center = Offset(centerX + radius * 0.70f, centerY + radius * 0.16f))

    // Mouth
    val mouthTop = centerY + radius * 0.52f
    when (mood) {
        MascotMood.HAPPY, MascotMood.EXCITED -> drawArc(
            color = look.outline,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(centerX - radius * 0.26f, mouthTop - radius * 0.28f),
            size = Size(radius * 0.52f, radius * 0.52f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.07f)
        )
        MascotMood.THINKING -> drawLine(
            color = look.outline,
            start = Offset(centerX - radius * 0.16f, mouthTop),
            end = Offset(centerX + radius * 0.16f, mouthTop),
            strokeWidth = radius * 0.07f
        )
        MascotMood.SLEEPY -> drawArc(
            color = look.outline,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(centerX - radius * 0.26f, mouthTop - radius * 0.10f),
            size = Size(radius * 0.52f, radius * 0.52f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.07f)
        )
    }
}

/** Decorative floating stars around a mascot or a title. */
@Composable
fun TwinklingStars(modifier: Modifier = Modifier, count: Int = 6, tint: Color = Palette.SunYellow) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "twinkle")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle_phase"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (i in 0 until count) {
            val angle = (PI * 2.0 / count) * i
            val distance = kotlin.math.min(w, h) * 0.44f
            val scale = if (reduceMotion) 1f else 0.7f + (sin(phase * PI.toFloat() * 2f + i) * 0.3f)
            val cx = w / 2f + (distance * cos(angle)).toFloat()
            val cy = h / 2f + (distance * sin(angle)).toFloat()
            drawStar(
                center = Offset(cx, cy),
                radius = kotlin.math.min(w, h) * 0.05f * scale,
                color = tint.copy(alpha = 0.9f)
            )
        }
    }
}
