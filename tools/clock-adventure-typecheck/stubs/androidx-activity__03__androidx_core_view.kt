package androidx.core.view

import android.view.View
import android.view.Window

object WindowCompat {
    fun setDecorFitsSystemWindows(window: Window, decorFitsSystemWindows: Boolean) {}
    fun getInsetsController(window: Window, view: View): WindowInsetsControllerCompat = WindowInsetsControllerCompat(window, view)
}

class WindowInsetsControllerCompat(window: Window, view: View) {
    fun hide(type: Int) {}
    fun show(type: Int) {}
    var systemBarsBehavior: Int = 0

    companion object {
        const val BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 1
    }
}

object WindowInsetsCompat {
    object Type {
        fun systemBars(): Int = 1
        fun navigationBars(): Int = 2
        fun statusBars(): Int = 4
    }
}
