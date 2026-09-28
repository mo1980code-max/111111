// Overrides the sibling harness' one line Lifecycle stub: this app observes real events.
package androidx.lifecycle

interface LifecycleOwner {
    val lifecycle: Lifecycle
}

open class Lifecycle {
    enum class State { DESTROYED, INITIALIZED, CREATED, STARTED, RESUMED }

    enum class Event {
        ON_CREATE, ON_START, ON_RESUME, ON_PAUSE, ON_STOP, ON_DESTROY, ON_ANY
    }

    open val currentState: State = State.INITIALIZED
    open fun addObserver(observer: Any) {}
    open fun removeObserver(observer: Any) {}
}

fun interface LifecycleEventObserver {
    fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event)
}

fun interface DefaultLifecycleObserver {
    fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event)
}
