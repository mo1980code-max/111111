package androidx.activity.result

class ActivityResultLauncher<I> {
    fun launch(input: I?) {}
    fun unregister() {}
}

class ActivityResultCaller
