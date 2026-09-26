package com.clockadventure.presentation.clock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockadventure.domain.engine.ClockMath
import com.clockadventure.domain.engine.TimeFormatter
import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.ClockHand
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.presentation.theme.ClockPalette
import com.clockadventure.presentation.theme.clockPaletteFor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.drawscope.drawLine
import androidx.compose.ui.graphics.drawscope.drawOval
import androidx.compose.ui.graphics.drawscope.drawRoundRect
import androidx.compose.ui.unit.times

/**
 * The interactive analog clock - the heart of the app.
 *
 * Everything is drawn on a Canvas: the bevelled bezel, the gradient face, the tick marks, the
 * glass highlight and the two hands with their shadows, which is what gives it the toy-like 3D
 * look without shipping a single image.
 *
 * Touch handling is native Compose: [detectDragGestures] converts the pointer position into an
 * angle, decides whether the child grabbed the long or the short hand, snaps the value to the
 * granularity of the current level and reports the new time. Because the hour hand angle is
 * derived from the minutes, it always follows the minute hand like a real clock.
 */
@Composable
fun InteractiveClock(
    time: ClockTime,
    modifier: Modifier = Modifier,
    style: com.clockadventure.domain.model.ClockStyle = com.clockadventure.domain.model.ClockStyle.CANDY,
    interactive: Boolean = false,
    /** Snap granularity of the minute hand: 60 = hours, 30 = halves, 15 = quarters, 5, 1. */
    snapMinutes: Int = 5,
    showNumbers: Boolean = true,
    showDigital: Boolean = false,
    use24Hour: Boolean = false,
    language: AppLanguage = AppLanguage.ENGLISH,
    /** Which hand to highlight as a hint. */
    hintHand: ClockHand? = null,
    /** Draws the correct answer as translucent "ghost" hands. */
    ghostTime: ClockTime? = null,
    /** Pops the clock when an answer was right. */
    successPulse: Boolean = false,
    reduceMotion: Boolean = false,
    /** Semantics tag for the instrumented tests (see presentation/src/androidTest). */
    testTag: String? = null,
    onTimeChanged: ((ClockTime) -> Unit)? = null
) {
    val palette = clockPaletteFor(style)
    val pulse by animateFloatAsState(
        targetValue = if (successPulse) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 220f),
        label = "clock_pulse"
    )
    var grabbedHand by remember { mutableStateOf<ClockHand?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatioCompat()
            .shadow(elevation = 18.dp, shape = CircleShape, clip = false),
        contentAlignment = Alignment.Center
    ) {
        val sizePx = with(LocalDensity.current) { maxWidth.toPx() }
        val center = Offset(sizePx / 2f, sizePx / 2f)
        val numberBox = maxWidth * 0.16f
        val numberBoxPx = with(LocalDensity.current) { numberBox.toPx() }
        val scale = if (reduceMotion) 1f else pulse

        Canvas(
            modifier = Modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .size(maxWidth)
                .then(
                    if (interactive && onTimeChanged != null) {
                        Modifier.pointerInput(time, snapMinutes) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val angle = ClockMath.angleOfPoint(center.x, center.y, offset.x, offset.y)
                                    grabbedHand = if (
                                        angularDistance(angle, time.minuteAngle) <= angularDistance(angle, time.hourAngle)
                                    ) ClockHand.MINUTE else ClockHand.HOUR
                                },
                                onDragEnd = { grabbedHand = null },
                                onDragCancel = { grabbedHand = null },
                                onDrag = { change, _ ->
                                    val angle = ClockMath.angleOfPoint(center.x, center.y, change.position.x, change.position.y)
                                    when (grabbedHand) {
                                        ClockHand.MINUTE -> {
                                            val minute = ClockMath.snapMinute(ClockMath.minuteFromAngle(angle), snapMinutes)
                                            onTimeChanged(ClockTime.of12(time.hour12, minute, pm = time.isPm))
                                        }
                                        ClockHand.HOUR -> {
                                            val hourNumber = ClockMath.hourNumberFromAngle(angle)
                                            onTimeChanged(ClockTime.of12(hourNumber, time.minute, pm = time.isPm))
                                        }
                                        null -> Unit
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            drawClockFace(
                center = center,
                radius = min(size.width, size.height) / 2f * 0.94f * scale,
                palette = palette,
                success = successPulse,
                grabbedHand = grabbedHand
            )
            if (ghostTime != null) {
                drawHands(
                    center = center,
                    radius = min(size.width, size.height) / 2f * 0.94f * scale,
                    palette = palette,
                    time = ghostTime,
                    ghost = true,
                    highlight = null
                )
            }
            drawHands(
                center = center,
                radius = min(size.width, size.height) / 2f * 0.94f * scale,
                palette = palette,
                time = time,
                ghost = false,
                highlight = hintHand,
                grabbed = grabbedHand
            )
        }

        if (showNumbers) {
            for (number in 1..12) {
                val angle = number * 30f
                val radians = Math.toRadians(ClockMath.clockAngleToCanvasDegrees(angle).toDouble())
                val radius = sizePx * 0.34f * scale
                val x = center.x + (radius * cos(radians)).toFloat() - numberBoxPx / 2f
                val y = center.y + (radius * sin(radians)).toFloat() - numberBoxPx / 2f
                Box(
                    modifier = Modifier
                        .size(numberBox)
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (maxWidth.value * 0.11f).sp
                        ),
                        color = palette.number,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showDigital) {
            DigitalBadge(
                text = TimeFormatter.digital(time, use24Hour, language),
                palette = palette,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** `aspectRatio` on a Box needs a modifier only; kept separate so the call site stays readable. */
private fun Modifier.aspectRatioCompat(): Modifier = this.then(Modifier.aspectRatio(1f))

@Composable
private fun DigitalBadge(text: String, palette: ClockPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(y = 12.dp)
            .shadow(elevation = 8.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(50), clip = false)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .backgroundCompat(palette.rim)
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = palette.faceTop
        )
    }
}

private fun Modifier.backgroundCompat(color: Color): Modifier =
    this.then(androidx.compose.ui.Modifier.background(color))

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClockFace(
    center: Offset,
    radius: Float,
    palette: ClockPalette,
    success: Boolean,
    grabbedHand: ClockHand?
) {
    // Bezel: a lit ring that makes the clock look moulded rather than printed.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.bezelLight, palette.bezelDark),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.30f),
            radius = radius * 1.45f
        ),
        radius = radius,
        center = center
    )
    if (success || grabbedHand != null) {
        drawCircle(
            color = palette.rim.copy(alpha = 0.45f),
            radius = radius * 1.06f,
            center = center,
            style = Stroke(width = radius * 0.05f)
        )
    }
    // Face
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.faceTop, palette.faceBottom),
            center = Offset(center.x - radius * 0.18f, center.y - radius * 0.22f),
            radius = radius * 1.25f
        ),
        radius = radius * 0.90f,
        center = center
    )
    // Inner rim
    drawCircle(
        color = palette.rim,
        radius = radius * 0.90f,
        center = center,
        style = Stroke(width = radius * 0.035f)
    )

    // Ticks: 60 minutes, every fifth one thicker.
    for (minute in 0 until 60) {
        val isMajor = minute % 5 == 0
        val angle = ClockMath.clockAngleToCanvasDegrees(minute * 6f)
        val radians = Math.toRadians(angle.toDouble())
        val outer = radius * 0.83f
        val inner = if (isMajor) radius * 0.70f else radius * 0.77f
        drawLine(
            color = if (isMajor) palette.tickMajor else palette.tick,
            start = Offset(
                x = center.x + (inner * cos(radians)).toFloat(),
                y = center.y + (inner * sin(radians)).toFloat()
            ),
            end = Offset(
                x = center.x + (outer * cos(radians)).toFloat(),
                y = center.y + (outer * sin(radians)).toFloat()
            ),
            strokeWidth = if (isMajor) radius * 0.045f else radius * 0.018f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    // Glass highlight
    drawCircle(
        color = palette.glass,
        radius = radius * 0.62f,
        center = Offset(center.x - radius * 0.20f, center.y - radius * 0.34f),
        alpha = 0.55f
    )

    // Center cap
    drawCircle(color = palette.center, radius = radius * 0.075f, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = radius * 0.032f,
        center = Offset(center.x - radius * 0.02f, center.y - radius * 0.02f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHands(
    center: Offset,
    radius: Float,
    palette: ClockPalette,
    time: ClockTime,
    ghost: Boolean,
    highlight: ClockHand?,
    grabbed: ClockHand? = null
) {
    val alpha = if (ghost) 0.42f else 1f
    val hourLength = radius * 0.52f
    val minuteLength = radius * 0.76f
    val hourWidth = radius * if (highlight == ClockHand.HOUR || grabbed == ClockHand.HOUR) 0.115f else 0.085f
    val minuteWidth = radius * if (highlight == ClockHand.MINUTE || grabbed == ClockHand.MINUTE) 0.10f else 0.070f

    drawHand(
        center = center,
        angle = time.hourAngle,
        length = hourLength,
        width = hourWidth,
        color = palette.hourHand,
        shadowColor = palette.handShadow,
        alpha = alpha
    )
    drawHand(
        center = center,
        angle = time.minuteAngle,
        length = minuteLength,
        width = minuteWidth,
        color = palette.minuteHand,
        shadowColor = palette.handShadow,
        alpha = alpha
    )

    if (!ghost) {
        drawCircle(color = palette.center, radius = radius * 0.058f, center = center, alpha = alpha)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHand(
    center: Offset,
    angle: Float,
    length: Float,
    width: Float,
    color: Color,
    shadowColor: Color,
    alpha: Float
) {
    rotate(degrees = ClockMath.clockAngleToCanvasDegrees(angle), pivot = center) {
        // Soft shadow slightly offset, which reads as depth.
        drawRoundRect(
            color = shadowColor.copy(alpha = 0.35f * alpha),
            topLeft = Offset(center.x - width / 2f + width * 0.35f, center.y - length + width * 0.30f),
            size = Size(width, length + width * 0.75f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2f, width / 2f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.82f)),
                startY = center.y - length,
                endY = center.y + width * 0.75f
            ),
            topLeft = Offset(center.x - width / 2f, center.y - length),
            size = Size(width, length + width * 0.75f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2f, width / 2f)
        )
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = width * 0.78f,
            center = Offset(center.x, center.y - length + width * 0.55f)
        )
    }
}

/** Distance between two clock angles, 0..180. */
private fun angularDistance(a: Float, b: Float): Float {
    val diff = abs(a - b) % 360f
    return if (diff > 180f) 360f - diff else diff
}

/** Small non interactive clock used by the Match game and the previews. */
@Composable
fun MiniClock(
    time: ClockTime,
    modifier: Modifier = Modifier,
    style: com.clockadventure.domain.model.ClockStyle = com.clockadventure.domain.model.ClockStyle.CANDY,
    size: Dp = 120.dp,
    showNumbers: Boolean = false
) {
    InteractiveClock(
        time = time,
        modifier = modifier.size(size),
        style = style,
        interactive = false,
        showNumbers = showNumbers
    )
}

internal val LocalIsRtl
    @Composable
    get() = LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
