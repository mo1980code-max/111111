package androidx.compose.runtime

@Composable
fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsState(
    initial: T,
    context: Any? = null
): State<T> = androidx.compose.runtime.StubFlowState(initial)
