package com.clock.livewallpaper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.clock.livewallpaper.data.prefs.OverlayPositionOption
import com.clock.livewallpaper.data.prefs.OverlaySettings
import com.clock.livewallpaper.overlay.DhikrOverlayCard
import com.clock.livewallpaper.ui.theme.LocalDhikrColors

/**
 * Live preview of the floating card.
 *
 * The frame imitates "some other app" with a few neutral bars drawn in Compose, and the card
 * inside it is the real [DhikrOverlayCard] - same palette, same font scale, same opacity, same
 * geometry - only non-interactive, so a tap here never dismisses anything.
 */
@Composable
fun OverlayPreviewCard(
    text: String,
    settings: OverlaySettings,
    modifier: Modifier = Modifier,
    description: String? = null,
    height: androidx.compose.ui.unit.Dp = 260.dp
) {
    val shape = RoundedCornerShape(28.dp)
    val hairline = LocalDhikrColors.current.hairline
    val mock = Color(0xFF8A9791)

    val alignment = when (settings.position) {
        OverlayPositionOption.TOP -> Alignment.TopCenter
        OverlayPositionOption.CENTER -> Alignment.Center
        OverlayPositionOption.BOTTOM -> Alignment.BottomCenter
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE9E5DC), Color(0xFFD9D5CB))
                )
            )
            .border(1.dp, hairline, shape)
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                }
            ),
        contentAlignment = alignment
    ) {
        // Neutral "host app" content behind the card.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val pad = 22.dp.toPx()
            val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            val widths = listOf(0.52f, 0.78f, 0.66f, 0.84f, 0.44f, 0.72f)
            var y = pad
            widths.forEach { factor ->
                val barWidth = (w - pad * 2) * factor
                drawRoundRect(
                    color = mock.copy(alpha = 0.28f),
                    topLeft = Offset(w - pad - barWidth, y),
                    size = Size(barWidth, 10.dp.toPx()),
                    cornerRadius = radius
                )
                y += 30.dp.toPx()
            }
        }

        DhikrOverlayCard(
            text = text,
            settings = settings,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            interactive = false,
            showHint = true
        )
    }
}
