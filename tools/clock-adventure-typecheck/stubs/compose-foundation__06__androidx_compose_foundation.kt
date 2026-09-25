package androidx.compose.foundation

import androidx.compose.ui.Modifier

class ScrollState(initial: Int = 0) {
    var value: Int = initial
    suspend fun animateScrollTo(value: Int, animationSpec: Any? = null) {}
    suspend fun scrollTo(value: Int) {}
}

fun Modifier.verticalScroll(state: ScrollState, enabled: Boolean = true): Modifier = this.then(Modifier)
fun Modifier.horizontalScroll(state: ScrollState, enabled: Boolean = true): Modifier = this.then(Modifier)
fun rememberScrollState(initial: Int = 0): ScrollState = ScrollState(initial)
