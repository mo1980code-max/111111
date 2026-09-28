package android.graphics.drawable

open class Drawable

open class GradientDrawable : Drawable() {
    var cornerRadius: Float = 0f
    open fun setColor(color: Int) {}
    open fun setStroke(width: Int, color: Int) {}
}
