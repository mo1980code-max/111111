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
