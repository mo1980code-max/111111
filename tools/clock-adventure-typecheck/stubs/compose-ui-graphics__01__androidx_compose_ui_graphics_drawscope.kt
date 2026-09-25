package androidx.compose.ui.graphics.drawscope

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Density

interface DrawScope : Density {
    val size: Size
    val center: Offset
    val layoutDirection: androidx.compose.ui.unit.LayoutDirection
}

interface ContentDrawScope : DrawScope {
    fun drawContent()
}

fun Stroke(
    width: Float = 0.0f,
    miter: Float = 4.0f,
    cap: StrokeCap = StrokeCap.Butt,
    join: StrokeJoin = StrokeJoin.Miter,
    pathEffect: PathEffect? = null
): Stroke = Stroke(width, miter, cap, join, pathEffect)

fun DrawScope.drawLine(
    brush: Brush,
    start: Offset,
    end: Offset,
    strokeWidth: Float = 1f,
    cap: StrokeCap = StrokeCap.Butt,
    pathEffect: PathEffect? = null,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawLine(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float = 1f,
    cap: StrokeCap = StrokeCap.Butt,
    pathEffect: PathEffect? = null,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawRect(
    color: Color,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawRect(
    brush: Brush,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawRoundRect(
    color: Color,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    cornerRadius: CornerRadius = CornerRadius.Zero,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawRoundRect(
    brush: Brush,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    cornerRadius: CornerRadius = CornerRadius.Zero,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawCircle(
    color: Color,
    radius: Float = this.size.minDimension / 2f,
    center: Offset = this.center,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawCircle(
    brush: Brush,
    radius: Float = this.size.minDimension / 2f,
    center: Offset = this.center,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawOval(
    color: Color,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawOval(
    brush: Brush,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawArc(
    brush: Brush,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawPath(
    path: Path,
    color: Color,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawPath(
    path: Path,
    brush: Brush,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode
) = Unit

fun DrawScope.drawImage(
    image: ImageBitmap,
    topLeft: Offset = Offset.Zero,
    alpha: Float = 1f,
    style: Any? = null,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScopeDefaults.BlendMode,
    filterQuality: FilterQuality = DrawScopeDefaults.DefaultFilterQuality
) = Unit

inline fun DrawScope.rotate(
    degrees: Float,
    pivot: Offset = center,
    block: DrawScope.() -> Unit
) = block()

inline fun DrawScope.scale(
    scale: Float,
    pivot: Offset = center,
    block: DrawScope.() -> Unit
) = block()

inline fun DrawScope.translate(
    left: Float = 0f,
    top: Float = 0f,
    block: DrawScope.() -> Unit
) = block()

inline fun DrawScope.inset(
    left: Float = 0f,
    top: Float = 0f,
    right: Float = 0f,
    bottom: Float = 0f,
    block: DrawScope.() -> Unit
) = block()

inline fun DrawScope.withTransform(
    transformBlock: DrawScope.() -> Unit,
    drawBlock: DrawScope.() -> Unit
) = drawBlock()

object DrawScopeDefaults {
    val BlendMode: androidx.compose.ui.graphics.BlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver
    val DefaultFilterQuality: FilterQuality = FilterQuality.Low
}
