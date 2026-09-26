package kotlinx.coroutines.sync

class Mutex(locked: Boolean = false)

suspend inline fun <T> Mutex.withLock(owner: Any? = null, action: () -> T): T = TODO()
