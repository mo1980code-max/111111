package com.clock.livewallpaper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.R
import com.clock.livewallpaper.core.ArabicText
import com.clock.livewallpaper.ui.theme.LocalDhikrColors
import com.clock.livewallpaper.ui.theme.dhikrBodyStyle

/**
 * The tasbeeh control: one large circular target (far beyond the 48dp minimum), an animated
 * progress ring, a soft ping on every count and a press scale. The whole circle is the button.
 */
@Composable
fun TasbeehRing(
    count: Int,
    target: Int,
    label: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = LocalDhikrColors.current
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val ringColor = MaterialTheme.colorScheme.primary
    val accent = extras.gold

    val safeTarget = target.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = (count.toFloat() / safeTarget).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "tasbeehProgress"
    )
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "tasbeehPress"
    )
    val ping = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (count > 0) {
            ping.snapTo(0f)
            ping.animateTo(1f, animationSpec = tween(durationMillis = 460))
        }
    }

    val counterDescription = stringResource(
        R.string.tasbeeh_counter_description,
        ArabicText.digits(count),
        ArabicText.digits(safeTarget)
    )
    val tapLabel = stringResource(R.string.cd_counter_tap)

    Box(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onTap() }
                )
            }
            .semantics {
                contentDescription = counterDescription
                role = Role.Button
                // TalkBack announces the hint and can trigger the count without a real tap.
                onClick(label = tapLabel) {
                    onTap()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Soft ping that expands away from the ring on each count.
            val pingValue = ping.value
            if (pingValue < 1f) {
                drawCircle(
                    color = accent.copy(alpha = 0.25f * (1f - pingValue)),
                    radius = (size.minDimension / 2f) * (0.82f + 0.18f * pingValue),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            // Inner hairline: geometry, not decoration overload.
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = size.minDimension / 2f - stroke * 1.6f,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = dhikrBodyStyle(fontScale = 0.8f),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = ArabicText.digits(count),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.tasbeeh_of_target, ArabicText.digits(safeTarget)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
