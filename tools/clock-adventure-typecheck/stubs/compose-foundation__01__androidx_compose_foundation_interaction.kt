package androidx.compose.foundation.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.foundation.InteractionSource

class MutableInteractionSource : InteractionSource

@Composable
fun InteractionSource.collectIsPressedAsState(): State<Boolean> = TODO()

@Composable
fun InteractionSource.collectIsFocusedAsState(): State<Boolean> = TODO()
