// The framework widgets the native ad layout is built from.
package android.widget

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup

open class TextView(context: Context) : View(context) {
    var text: CharSequence? = null
    var textSize: Float = 14f
    var maxLines: Int = Int.MAX_VALUE
    var gravity: Int = 0
    var isAllCaps: Boolean = false
    var minHeight: Int = 0
    var minWidth: Int = 0
    open fun setTextColor(color: Int) {}
    open fun setTypeface(typeface: Typeface?, style: Int) {}
}

open class Button(context: Context) : TextView(context)

open class ImageView(context: Context) : View(context) {
    var scaleType: ScaleType = ScaleType.FIT_CENTER
    var adjustViewBounds: Boolean = false
    open fun setImageDrawable(drawable: Drawable?) {}

    enum class ScaleType { MATRIX, FIT_XY, FIT_START, FIT_CENTER, FIT_END, CENTER, CENTER_CROP, CENTER_INSIDE }
}

open class FrameLayout(context: Context) : ViewGroup(context) {
    open class LayoutParams(width: Int, height: Int) : ViewGroup.MarginLayoutParams(width, height)
}

open class LinearLayout(context: Context) : ViewGroup(context) {
    var orientation: Int = HORIZONTAL
    var gravity: Int = 0

    open class LayoutParams : ViewGroup.MarginLayoutParams {
        var weight: Float = 0f
        constructor(width: Int, height: Int) : super(width, height)
        constructor(width: Int, height: Int, weight: Float) : super(width, height) {
            this.weight = weight
        }
    }

    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }
}
