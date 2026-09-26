package android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle

open class Application : Context() {
    override fun onCreate() {}
}

open class Activity : Context() {
    val window: android.view.Window = android.view.Window()
    open fun onCreate(savedInstanceState: Bundle?) {}
    open fun onPause() {}
    open fun onResume() {}
    open fun onDestroy() {}
    open fun recreate() {}
    override fun startActivity(intent: Intent) {}
}

class NotificationChannel(id: String, name: CharSequence, importance: Int) {
    var description: String? = null
}

class NotificationManager {
    companion object {
        const val IMPORTANCE_DEFAULT = 3
        const val IMPORTANCE_HIGH = 4
    }
    fun createNotificationChannel(channel: NotificationChannel) {}
    fun notify(id: Int, notification: Notification) {}
}

class Notification {
    class Builder(context: Context, channelId: String) {
        fun setSmallIcon(icon: Int): Builder = this
        fun setContentTitle(title: CharSequence): Builder = this
        fun setContentText(text: CharSequence): Builder = this
        fun setAutoCancel(cancel: Boolean): Builder = this
        fun setContentIntent(intent: PendingIntent): Builder = this
        fun build(): Notification = Notification()
    }
}

class AlarmManager {
    companion object {
        const val RTC_WAKEUP = 0
        const val INTERVAL_DAY = 86_400_000L
    }
    fun setRepeating(type: Int, triggerAtMillis: Long, intervalMillis: Long, operation: PendingIntent) {}
    fun cancel(operation: PendingIntent) {}
    fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent) {}
}

class PendingIntent {
    companion object {
        const val FLAG_UPDATE_CURRENT = 1
        const val FLAG_IMMUTABLE = 2
        fun getBroadcast(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent = PendingIntent()
        fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent = PendingIntent()
    }
}

package android.view

class Window {
    val decorView: View = View()
    fun setFlags(flags: Int, mask: Int) {}
}

class View {
    val context: android.content.Context = android.content.Context()
}
