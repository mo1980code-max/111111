package androidx.compose.ui.geometry

data class Offset(val x: Float, val y: Float) {
    companion object {
        val Zero = Offset(0f, 0f)
        val Infinite = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        val Unspecified = Offset(Float.NaN, Float.NaN)
    }
}

data class Size(val width: Float, val height: Float) {
    val minDimension: Float get() = kotlin.math.min(width, height)
    val maxDimension: Float get() = kotlin.math.max(width, height)

    companion object {
        val Zero = Size(0f, 0f)
        val Unspecified = Size(Float.NaN, Float.NaN)
    }
}

data class CornerRadius(val x: Float, val y: Float = x) {
    companion object {
        val Zero = CornerRadius(0f, 0f)
    }
}
