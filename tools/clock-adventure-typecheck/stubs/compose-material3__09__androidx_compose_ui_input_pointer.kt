package androidx.compose.ui.input.pointer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

interface PointerInputScope {
    val size: androidx.compose.ui.geometry.Size
    val viewConfiguration: Any
}

class PointerInputChange {
    val position: Offset get() = Offset.Zero
    val previousPosition: Offset get() = Offset.Zero
}

fun Modifier.pointerInput(vararg keys: Any?, block: suspend PointerInputScope.() -> Unit): Modifier = Modifier
fun Modifier.pointerInput(key1: Any?, block: suspend PointerInputScope.() -> Unit): Modifier = Modifier
