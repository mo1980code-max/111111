package com.clockadventure.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clockadventure.presentation.theme.Dimens
import com.clockadventure.presentation.theme.Palette
import com.clockadventure.presentation.theme.appColors
import androidx.compose.foundation.background

/**
 * The big arcade button of the app.
 *
 * It is a real 3D object: a pill with a three stop gradient, a drop shadow and a press animation
 * that pushes it down by a few pixels, so children get physical feedback when they tap it.
 */
@Composable
fun ArcadeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = Dimens.buttonHeight,
    cornerRadius: Dp = 36.dp,
    topColor: Color = appColors.accent,
    bottomColor: Color = appColors.accentDark,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp),
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(targetValue = if (pressed) 2.dp else 10.dp, label = "button_elevation")
    val offsetY by animateDpAsState(targetValue = if (pressed) 5.dp else 0.dp, label = "button_offset")
    val shape = RoundedCornerShape(cornerRadius)
    val background = if (enabled) {
        Brush.verticalGradient(listOf(topColor, topColor, bottomColor))
    } else {
        Brush.verticalGradient(listOf(Palette.Locked, Palette.Locked.copy(alpha = 0.85f)))
    }

    Box(
        modifier = modifier
            .height(height)
            .shadow(elevation = elevation, shape = shape, clip = false)
            .offset(y = offsetY)
            .clip(shape)
            .background(background)
            .then(
                if (enabled) {
                    Modifier.rippleClickable(interactionSource) { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = textStyle.copy(textAlign = TextAlign.Center),
                color = Color.White
            )
        }
    }
}

/**
 * `Modifier.clickable` with the material ripple, kept in one place so the custom buttons all
 * behave like the rest of the platform.
 */
private fun Modifier.rippleClickable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
): Modifier = this.clickable(
    interactionSource = interactionSource,
    indication = ripple(),
    onClick = onClick
)

/** Small round icon button used in the top bars. */
@Composable
fun RoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .clip(shape)
            .background(containerColor)
            .rippleClickable(remember { MutableInteractionSource() }) { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

/** Wide choice button used for answers. */
@Composable
fun AnswerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: AnswerState = AnswerState.IDLE,
    enabled: Boolean = true
) {
    val colors = when (state) {
        AnswerState.IDLE -> appColors.accent to appColors.accentDark
        AnswerState.CORRECT -> Color(0xFF3ED9A3) to Color(0xFF1E9E6E)
        AnswerState.WRONG -> Color(0xFFFF8A8A) to Color(0xFFE04040)
        AnswerState.HINT -> Palette.SunYellow to Color(0xFFE39B00)
    }
    ArcadeButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(64.dp),
        enabled = enabled,
        height = 64.dp,
        topColor = colors.first,
        bottomColor = colors.second,
        cornerRadius = 22.dp
    )
}

enum class AnswerState { IDLE, CORRECT, WRONG, HINT }
