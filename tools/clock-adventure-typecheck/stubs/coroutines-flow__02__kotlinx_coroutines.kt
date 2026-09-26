package kotlinx.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Job {
    fun cancel() {}
    val isActive: Boolean get() = true
}

suspend fun delay(timeMillis: Long) {}

fun CoroutineScope.launch(
    context: Any? = null,
    start: Any? = null,
    block: suspend CoroutineScope.() -> Unit
): Job = Job()

fun <T> Flow<T>.launchIn(scope: CoroutineScope): Job = Job()

suspend fun coroutineScope(block: suspend CoroutineScope.() -> Unit) {}
