package kotlinx.coroutines

interface CoroutineScope {
    val coroutineContext: Any
    val isActive: Boolean get() = true
}

interface CoroutineContext

object Dispatchers {
    val Default: Any = Any()
    val Main: Any = Any()
    val IO: Any = Any()
    val Immediate: Any = Any()
}
