package androidx.lifecycle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineContext

open class ViewModel {
    protected open fun onCleared() {}
    internal fun clear() { onCleared() }
}

internal class StubCoroutineScope : CoroutineScope {
    override val coroutineContext: CoroutineContext = object : CoroutineContext {}
}

val ViewModel.viewModelScope: CoroutineScope
    get() = StubCoroutineScope()

class SavedStateHandle {
    private val values = HashMap<String, Any?>()
    fun <T> get(key: String): T? = values[key] as? T
    operator fun <T> set(key: String, value: T?) { values[key] = value }
    fun <T> getStateFlow(key: String, initialValue: T): kotlinx.coroutines.flow.StateFlow<T> =
        kotlinx.coroutines.flow.MutableStateFlow(initialValue)
    fun contains(key: String): Boolean = values.containsKey(key)
}
