package kotlinx.coroutines.flow

interface Flow<out T>

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): Flow<T> = TODO()

fun <T> flowOf(vararg values: T): Flow<T> = TODO()

suspend fun <T> Flow<T>.collect(action: suspend (T) -> Unit) {}

inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}

fun <T, R> Flow<T>.map(transform: suspend (T) -> R): Flow<R> = TODO()

fun <T> Flow<T>.filter(predicate: suspend (T) -> Boolean): Flow<T> = TODO()

fun <T> Flow<T>.onEach(action: suspend (T) -> Unit): Flow<T> = TODO()

fun <T> Flow<T>.catch(action: suspend FlowCollector<T>.(Throwable) -> Unit): Flow<T> = TODO()

fun <T> Flow<T>.distinctUntilChanged(): Flow<T> = TODO()

fun <T> Flow<T>.onStart(action: suspend () -> Unit): Flow<T> = TODO()

fun <T> Flow<T>.flowOn(context: Any?): Flow<T> = TODO()

fun <T> Flow<T>.conflate(): Flow<T> = TODO()

fun <T1, T2, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    transform: suspend (T1, T2) -> R
): Flow<R> = TODO()

fun <T1, T2, T3, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    transform: suspend (T1, T2, T3) -> R
): Flow<R> = TODO()

fun <T1, T2, T3, T4, T5, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    transform: suspend (T1, T2, T3, T4, T5) -> R
): Flow<R> = TODO()

fun <T1, T2, T3, T4, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    transform: suspend (T1, T2, T3, T4) -> R
): Flow<R> = TODO()

interface StateFlow<out T> : Flow<T> {
    val value: T
}

interface MutableStateFlow<T> : StateFlow<T> {
    override var value: T
    fun compareAndSet(expect: T, update: T): Boolean
    fun asStateFlow(): StateFlow<T> = this
}

fun <T> MutableStateFlow(value: T): MutableStateFlow<T> = TODO()

class SharingStarted {
    companion object {
        val Eagerly: Any = Any()
        val Lazily: Any = Any()
        fun WhileSubscribed(stopTimeoutMillis: Long = 0L, replayExpirationMillis: Long = 0L): Any = Any()
    }
}

fun <T> Flow<T>.stateIn(scope: Any?, started: Any, initialValue: T): StateFlow<T> = MutableStateFlow(initialValue)

suspend fun <T> Flow<T>.first(): T = TODO()
