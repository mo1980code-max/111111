package androidx.compose.ui.unit

import androidx.compose.ui.Modifier

class Dp(val value: Float) : Comparable<Dp> {
    companion object {
        val Unspecified: Dp = Dp(Float.NaN)
        val Hairline: Dp = Dp(0f)
        val Infinity: Dp = Dp(Float.POSITIVE_INFINITY)
    }

    override fun compareTo(other: Dp): Int = value.compareTo(other.value)
}


class TextUnit(val value: Float)
class IntOffset(val x: Int, val y: Int)
class IntSize(val width: Int, val height: Int)
class DpSize(val width: Dp, val height: Dp)

val Float.dp: Dp get() = Dp(this)
val Int.dp: Dp get() = Dp(this.toFloat())
val Double.dp: Dp get() = Dp(this.toFloat())
val Float.sp: TextUnit get() = TextUnit(this)
val Int.sp: TextUnit get() = TextUnit(this.toFloat())

operator fun Dp.unaryMinus(): Dp = Dp(-this.value)
operator fun Dp.plus(other: Dp): Dp = Dp(this.value + other.value)
operator fun Dp.minus(other: Dp): Dp = Dp(this.value - other.value)
operator fun Dp.times(factor: Float): Dp = Dp(this.value * factor)
operator fun Dp.times(factor: Int): Dp = Dp(this.value * factor)
operator fun Dp.div(factor: Float): Dp = Dp(this.value / factor)
operator fun Dp.compareTo(other: Dp): Int = this.value.compareTo(other.value)

enum class LayoutDirection { Ltr, Rtl }

interface Density {
    val density: Float
    val fontScale: Float

    fun Dp.toPx(): Float = value * density
    fun Dp.roundToPx(): Int = (value * density).toInt()
    fun Dp.toSp(): TextUnit = TextUnit(value * density)
    fun Float.toDp(): Dp = Dp(this / density)
    fun Int.toDp(): Dp = Dp(this / density)
    fun Float.toSp(): TextUnit = TextUnit(this / fontScale)
    fun Int.toSp(): TextUnit = TextUnit(this / fontScale)
    fun TextUnit.toDp(): Dp = Dp(value / density)
    fun TextUnit.toSp(): TextUnit = this
}

fun Density.toPx(dp: Dp): Float = dp.value * density
fun Density.toSp(dp: Dp): TextUnit = TextUnit(dp.value * density)
fun Dp.toPx(density: Density): Float = value * density.density
