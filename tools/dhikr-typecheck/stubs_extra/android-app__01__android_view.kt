// Overrides the thin android.view stub of the sibling harness: this app builds real Views.
package android.view

import android.content.Context
import android.graphics.drawable.Drawable

class Window {
    val decorView: View = View(Context())
    fun setFlags(flags: Int, mask: Int) {}
}

interface ViewParent

open class View(val context: Context) {
    var id: Int = 0
    var tag: Any? = null
    var visibility: Int = VISIBLE
    var background: Drawable? = null
    var layoutParams: ViewGroup.LayoutParams? = null
    var textAlignment: Int = TEXT_ALIGNMENT_INHERIT
    var minimumHeight: Int = 0
    var contentDescription: CharSequence? = null
    open val parent: ViewParent? = null
    open fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {}
    open fun setOnClickListener(listener: OnClickListener?) {}
    open fun findViewById(id: Int): View? = null

    fun interface OnClickListener {
        fun onClick(view: View)
    }

    companion object {
        const val VISIBLE = 0
        const val INVISIBLE = 4
        const val GONE = 8
        const val TEXT_ALIGNMENT_INHERIT = 0
        const val TEXT_ALIGNMENT_VIEW_START = 5
        const val TEXT_DIRECTION_LOCALE = 5
    }
}

open class ViewGroup(context: Context) : View(context), ViewParent {
    open fun addView(child: View) {}
    open fun addView(child: View, params: LayoutParams) {}
    open fun removeView(child: View) {}
    open fun removeAllViews() {}
    open fun getChildAt(index: Int): View? = null

    open class LayoutParams(var width: Int, var height: Int) {
        companion object {
            const val MATCH_PARENT = -1
            const val WRAP_CONTENT = -2
        }
    }

    open class MarginLayoutParams(width: Int, height: Int) : LayoutParams(width, height) {
        var topMargin: Int = 0
        var bottomMargin: Int = 0
        var marginStart: Int = 0
        var marginEnd: Int = 0
    }
}

object Gravity {
    const val CENTER = 17
    const val CENTER_VERTICAL = 16
    const val CENTER_HORIZONTAL = 1
    const val START = 8388611
    const val END = 8388613
    const val TOP = 48
    const val BOTTOM = 80
}
