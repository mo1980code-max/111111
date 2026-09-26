package androidx.compose.ui

import androidx.compose.ui.unit.LayoutDirection

class Alignment(val value: Int) {
    companion object {
        val TopStart: Alignment = Alignment(0)
        val TopCenter: Alignment = Alignment(1)
        val TopEnd: Alignment = Alignment(2)
        val CenterStart: Alignment = Alignment(3)
        val Center: Alignment = Alignment(4)
        val CenterEnd: Alignment = Alignment(5)
        val BottomStart: Alignment = Alignment(6)
        val BottomCenter: Alignment = Alignment(7)
        val BottomEnd: Alignment = Alignment(8)
        val Start: Any = Alignment(9)
        val End: Any = Alignment(10)
        val Top: Any = Alignment(11)
        val Bottom: Any = Alignment(12)
        val CenterHorizontally: Any = Alignment(13)
        val CenterVertically: Any = Alignment(14)
    }
}
