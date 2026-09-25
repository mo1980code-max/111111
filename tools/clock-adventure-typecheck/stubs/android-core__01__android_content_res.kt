package android.content.res

class Resources {
    val configuration: Configuration = Configuration()
    val displayMetrics: android.util.DisplayMetrics = android.util.DisplayMetrics()
    @Suppress("DEPRECATION")
    fun updateConfiguration(config: Configuration, metrics: android.util.DisplayMetrics) {}
    fun getString(id: Int): String = ""
}

class Configuration {
    val locales: android.os.LocaleList = android.os.LocaleList()
    fun setLocale(locale: java.util.Locale) {}
    fun setLayoutDirection(locale: java.util.Locale) {}
}

class AssetFileDescriptor(fd: android.os.ParcelFileDescriptor, startOffset: Long, length: Long) {
    val fileDescriptor: java.io.FileDescriptor? = null
}
