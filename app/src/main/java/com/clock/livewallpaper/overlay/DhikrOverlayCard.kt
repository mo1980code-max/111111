package com.clock.livewallpaper.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clock.livewallpaper.R
import com.clock.livewallpaper.data.prefs.OverlaySettings
import com.clock.livewallpaper.ui.theme.DhikrUiFont
import com.clock.livewallpaper.ui.theme.dhikrBodyStyle
import com.clock.livewallpaper.ui.theme.overlayPalette

/**
 * The floating dhikr card.
 *
 * The very same composable renders the real window above other apps and the live preview inside
 * "مظهر بطاقة الذكر", so a style, size, opacity or position change can never drift between them.
 * It takes no theme from its host: every colour and text style is explicit, because the real card
 * is composed inside a bare WindowManager view.
 */
@Composable
fun DhikrOverlayCard(
    text: String,
    settings: OverlaySettings,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    showHint: Boolean = true,
    hintText: String = stringResource(R.string.overlay_dismiss_hint),
    onDismiss: () -> Unit = {}
) {
    val palette = overlayPalette(settings.style)
    val cardDescription = stringResource(R.string.overlay_card_description)
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "overlayPressScale"
    )

    val shape = RoundedCornerShape(OVERLAY_CORNER_DP.dp)
    val touchModifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    // Touch anywhere: dismissal starts on the DOWN event, nothing is launched.
                    pressed = true
                    onDismiss()
                    tryAwaitRelease()
                    pressed = false
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = palette.shadow,
                spotColor = palette.shadow
            )
            .clip(shape)
            .background(palette.surface.copy(alpha = palette.surface.alpha * settings.opacity))
            .border(width = 1.dp, color = palette.border, shape = shape)
            .then(touchModifier)
            .semantics { contentDescription = cardDescription }
            .testTag("dhikr_overlay_card")
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OverlayOrnament(
                accent = palette.accent,
                line = palette.innerTint
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = text,
                style = dhikrBodyStyle(fontScale = settings.fontScale).copy(textAlign = TextAlign.Center),
                color = palette.text,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (showHint) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = hintText,
                    style = TextStyle(
                        fontFamily = DhikrUiFont,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = palette.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.9f)
                )
            }
        }
    }
}

/** A tiny original detail: two hairlines meeting a hollow diamond - geometric, never clip-art. */
@Composable
private fun OverlayOrnament(
    accent: Color,
    line: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .width(44.dp)
                .height(1.dp)
                .background(line)
        )
        Canvas(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(10.dp)
        ) {
            val side = size.minDimension
            val diamond = Path().apply {
                moveTo(side / 2f, 0f)
                lineTo(side, side / 2f)
                lineTo(side / 2f, side)
                lineTo(0f, side / 2f)
                close()
            }
            drawPath(path = diamond, color = accent, style = Stroke(width = 1.4.dp.toPx()))
        }
        Box(
            Modifier
                .width(44.dp)
                .height(1.dp)
                .background(line)
        )
    }
}

const val OVERLAY_CORNER_DP = 26
