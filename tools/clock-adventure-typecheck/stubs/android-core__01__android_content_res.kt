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
    var screenWidthDp: Int = 0
    var screenHeightDp: Int = 0
    var smallestScreenWidthDp: Int = 0
    var orientation: Int = ORIENTATION_PORTRAIT
    fun setLocale(locale: java.util.Locale) {}
    fun setLayoutDirection(locale: java.util.Locale) {}

    companion object {
        const val ORIENTATION_UNDEFINED = 0
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
    }
}

class AssetFileDescriptor(fd: android.os.ParcelFileDescriptor, startOffset: Long, length: Long) {
    val fileDescriptor: java.io.FileDescriptor? = null
}
