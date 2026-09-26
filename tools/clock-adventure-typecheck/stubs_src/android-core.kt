package android.content

import android.content.res.Resources
import android.os.Bundle

open class Context {
    open fun getString(id: Int): String = ""
    open fun getString(id: Int, vararg args: Any?): String = ""
    open fun getSystemService(name: String): Any? = null
    open fun getSystemService(cls: Class<*>): Any? = null
    open val resources: Resources = Resources()
    open val cacheDir: java.io.File = java.io.File("")
    open fun startActivity(intent: Intent) {}
    open fun sendBroadcast(intent: Intent) {}
    open val packageName: String = ""
    open fun onCreate() {}

    companion object {
        const val ALARM_SERVICE = "alarm"
        const val NOTIFICATION_SERVICE = "notification"
        const val AUDIO_SERVICE = "audio"
    }
}

open class ContextWrapper(base: Context) : Context()

open class Intent {
    var action: String? = null
    var flags: Int = 0
    constructor()
    constructor(context: Context, cls: Class<*>)
    constructor(action: String)

    companion object {
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val FLAG_ACTIVITY_NEW_TASK = 1
        const val FLAG_ACTIVITY_CLEAR_TOP = 2
    }
}

abstract class BroadcastReceiver {
    abstract fun onReceive(context: Context, intent: Intent)
}

class ComponentName(pkg: String, cls: String)

object PackageManager {
    const val PERMISSION_GRANTED = 0
    const val PERMISSION_DENIED = -1
}

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

package android.content.pm

class PackageManager {
    companion object {
        const val PERMISSION_GRANTED = 0
    }
}

/** Only the orientation-lock constants the activity needs to switch between phone and tablet. */
object ActivityInfo {
    const val SCREEN_ORIENTATION_UNSPECIFIED = -1
    const val SCREEN_ORIENTATION_PORTRAIT = 1
    const val SCREEN_ORIENTATION_USER = 2
}

package android.util

class DisplayMetrics

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

package android

object Manifest {
    object permission {
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        const val SCHEDULE_EXACT_ALARM = "android.permission.SCHEDULE_EXACT_ALARM"
        const val INTERNET = "android.permission.INTERNET"
    }
}
