package androidx.compose.runtime

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T> StateFlow<T>.collectAsState(
    initial: T,
    context: Any? = null
): State<T> = StubFlowState(initial)

internal class StubFlowState<T>(override val value: T) : State<T>
