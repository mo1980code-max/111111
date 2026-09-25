package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

class Color(val value: ULong) {
    fun copy(alpha: Float = 1f, red: Float = 1f, green: Float = 1f, blue: Float = 1f): Color = this
    companion object {
        val White = Color(0xFFFFFFFF)
        val Black = Color(0xFF000000)
        val Transparent = Color(0x00000000)
        val Unspecified = Color(0)
        val Red = Color(0xFFFF0000)
        val Green = Color(0xFF00FF00)
        val Blue = Color(0xFF0000FF)
        val Gray = Color(0xFF888888)
        val LightGray = Color(0xFFDDDDDD)
    }
}

fun Color(color: Long): Color = Color(color.toULong())
fun Color(color: Int): Color = Color(color.toLong() and 0xFFFFFFFFL)
fun Color(red: Float, green: Float, blue: Float, alpha: Float = 1f): Color = Color(0)

val Color.alpha: Float get() = 1f
val Color.red: Float get() = 1f
val Color.green: Float get() = 1f
val Color.blue: Float get() = 1f
fun Color.lerp(start: Color, stop: Color, fraction: Float): Color = start

object ColorFilters {
    fun tint(color: Color): ColorFilter = TODO()
}

class ColorFilter

enum class TileMode { Clamp, Repeated, Mirror, Decal }

enum class BlendMode { Clear, Src, Dst, SrcOver, DstOver, SrcIn, DstIn, SrcOut, DstOut }

abstract class Brush {
    companion object {
    fun linearGradient(
        colors: List<Color>,
        start: Offset = Offset.Zero,
        end: Offset = Offset.Infinite,
        tileMode: TileMode = TileMode.Clamp
    ): Brush = SolidColor(colors.first())

    fun linearGradient(
        vararg colorStops: Pair<Float, Color>,
        start: Offset = Offset.Zero,
        end: Offset = Offset.Infinite,
        tileMode: TileMode = TileMode.Clamp
    ): Brush = SolidColor(colorStops.first().second)

    fun verticalGradient(
        colors: List<Color>,
        startY: Float = 0f,
        endY: Float = Float.POSITIVE_INFINITY,
        tileMode: TileMode = TileMode.Clamp
    ): Brush = SolidColor(colors.first())

    fun verticalGradient(
        vararg colorStops: Pair<Float, Color>,
        startY: Float = 0f,
        endY: Float = Float.POSITIVE_INFINITY,
        tileMode: TileMode = TileMode.Clamp
    ): Brush = SolidColor(colorStops.first().second)

    fun horizontalGradient(
        colors: List<Color>,
        startX: Float = 0f,
        endX: Float = Float.POSITIVE_INFINITY,
        tileMode: TileMode = TileMode.Clamp
    ): Brush = SolidColor(colors.first())

    fun radialGradient(
        colors: List<Color>,
        center: Offset = Offset.Unspecified,
        radius: Float = Float.POSITIVE_INFINITY,
        tileMode: TileMode = TileMode.Clamp
    ): Brush = SolidColor(colors.first())

        fun sweepGradient(colors: List<Color>, center: Offset = Offset.Unspecified): Brush = SolidColor(colors.first())
    }
}

class SolidColor(val value: Color) : Brush()

class Path {
    fun moveTo(x: Float, y: Float) = Unit
    fun relativeMoveTo(dx: Float, dy: Float) = Unit
    fun lineTo(x: Float, y: Float) = Unit
    fun relativeLineTo(dx: Float, dy: Float) = Unit
    fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) = Unit
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = Unit
    fun arcTo(
        rect: androidx.compose.ui.geometry.Size = androidx.compose.ui.geometry.Size.Zero,
        startAngleDegrees: Float = 0f,
        sweepAngleDegrees: Float = 0f,
        forceMoveTo: Boolean = false
    ) = Unit
    fun addOval(oval: androidx.compose.ui.geometry.Size) = Unit
    fun addRoundRect(roundRect: androidx.compose.ui.geometry.Size) = Unit
    fun close() = Unit
    fun reset() = Unit
}

class Stroke(
    val width: Float = 0f,
    val miter: Float = 4f,
    val cap: StrokeCap = StrokeCap.Butt,
    val join: StrokeJoin = StrokeJoin.Miter,
    val pathEffect: PathEffect? = null
)

enum class StrokeCap { Butt, Round, Square }
enum class StrokeJoin { Miter, Round, Bevel }
class PathEffect

class PaintingStyle

enum class FilterQuality { None, Low, Medium, High }

class Shader

class ImageBitmap

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
