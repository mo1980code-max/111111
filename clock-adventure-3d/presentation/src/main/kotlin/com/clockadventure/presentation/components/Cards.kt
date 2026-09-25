package com.clockadventure.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.appColors
import androidx.compose.ui.graphics.drawscope.drawRoundRect

/**
 * The glossy "3D" card used everywhere: a vertical gradient body, a soft drop shadow and a glass
 * highlight on top, which together read as a plastic toy rather than a flat panel.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    topColor: Color = appColors.cardTop,
    bottomColor: Color = appColors.cardBottom,
    elevation: Dp = 10.dp,
    cornerRadius: Dp = Dimens.cornerLarge,
    gloss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .then(
                if (gloss) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.32f),
                            topLeft = Offset(size.width * 0.10f, size.height * 0.05f),
                            size = Size(size.width * 0.80f, size.height * 0.16f),
                            cornerRadius = CornerRadius(size.height * 0.08f, size.height * 0.08f)
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.cardPadding),
            content = content
        )
    }
}

/** A small pill used for badges such as "Best: 12" or "3 stars". */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = appColors.accent,
    contentColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = contentColor,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}
