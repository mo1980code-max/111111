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
