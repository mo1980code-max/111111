package kotlinx.coroutines.flow

/** The real library exposes this as a top level extension; the sibling stub had it as a member. */
fun <T> MutableStateFlow<T>.asStateFlow(): StateFlow<T> = this
