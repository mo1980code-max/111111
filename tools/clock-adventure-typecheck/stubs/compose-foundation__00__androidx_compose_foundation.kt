package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

@Composable
fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) = Unit

fun Modifier.background(color: Color, shape: Shape? = null): Modifier = this
fun Modifier.background(brush: Brush, shape: Shape? = null, alpha: Float = 1f): Modifier = this

interface Indication

interface InteractionSource

fun Modifier.clickable(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Any? = null,
    onClick: () -> Unit
): Modifier = this

fun Modifier.combinedClickable(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    onLongClickLabel: String? = null,
    onClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = this

fun Modifier.border(width: Dp, color: Color, shape: Shape? = null): Modifier = this
fun Modifier.border(width: Dp, brush: Brush, shape: Shape? = null): Modifier = this

@Composable
fun isSystemInDarkTheme(): Boolean = false
