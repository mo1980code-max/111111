package android.os

class Bundle {
    fun getString(key: String): String? = null
    fun putString(key: String, value: String?) {}
}

class LocaleList {
    fun get(index: Int): java.util.Locale = java.util.Locale.getDefault()
}

class ParcelFileDescriptor {
    companion object {
        const val MODE_READ_ONLY = 1
        fun open(file: java.io.File, mode: Int): ParcelFileDescriptor = ParcelFileDescriptor()
    }
}

object Build {
    object VERSION {
        const val SDK_INT = 35
    }
    object VERSION_CODES {
        const val M = 23
        const val O = 26
        const val TIRAMISU = 33
    }
}

class Handler {
    fun post(block: () -> Unit) {}
}
