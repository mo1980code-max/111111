// Copied from the sibling harness and extended: this app dispatches on Main.immediate.
package kotlinx.coroutines

interface CoroutineScope {
    val coroutineContext: Any
    val isActive: Boolean get() = true
}

interface CoroutineContext

open class CoroutineDispatcher

open class MainCoroutineDispatcher : CoroutineDispatcher() {
    open val immediate: MainCoroutineDispatcher get() = this
}

object Dispatchers {
    val Default: CoroutineDispatcher = CoroutineDispatcher()
    val Main: MainCoroutineDispatcher = MainCoroutineDispatcher()
    val IO: CoroutineDispatcher = CoroutineDispatcher()
    val Unconfined: CoroutineDispatcher = CoroutineDispatcher()
}
