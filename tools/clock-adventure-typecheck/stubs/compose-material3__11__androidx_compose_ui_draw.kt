package androidx.compose.ui.draw
import androidx.compose.ui.unit.dp

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp

fun Modifier.shadow(
    elevation: Dp,
    shape: Shape? = null,
    clip: Boolean = true,
    ambientColor: Color = Color(0xFF000000),
    spotColor: Color = Color(0xFF000000)
): Modifier = this

fun Modifier.clip(shape: Shape): Modifier = this
fun Modifier.alpha(alpha: Float): Modifier = this
fun Modifier.rotate(degrees: Float): Modifier = this
fun Modifier.scale(scaleX: Float, scaleY: Float = scaleX): Modifier = this
fun Modifier.drawWithContent(onDraw: ContentDrawScope.() -> Unit): Modifier = this
fun Modifier.drawBehind(onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit): Modifier = this
fun Modifier.drawWithCache(onBuildDrawCache: CacheDrawScope.() -> DrawResult): Modifier = this

class CacheDrawScope
class DrawResult

fun Modifier.blur(radius: Dp, edgeTreatment: Int = 0): Modifier = this
