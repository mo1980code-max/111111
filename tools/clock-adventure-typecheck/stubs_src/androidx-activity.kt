package androidx.activity

import android.os.Bundle

open class ComponentActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    val lifecycle: androidx.lifecycle.Lifecycle = androidx.lifecycle.Lifecycle()
}

package androidx.activity.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable

fun ComponentActivity.setContent(content: @Composable () -> Unit) = Unit

package androidx.lifecycle

class Lifecycle {
    fun addObserver(observer: Any) {}
}

package androidx.core.view

import android.view.View
import android.view.Window

object WindowCompat {
    fun setDecorFitsSystemWindows(window: Window, decorFitsSystemWindows: Boolean) {}
    fun getInsetsController(window: Window, view: View): WindowInsetsControllerCompat = WindowInsetsControllerCompat(window, view)
}

class WindowInsetsControllerCompat(window: Window, view: View) {
    fun hide(type: Int) {}
    fun show(type: Int) {}
    var systemBarsBehavior: Int = 0

    companion object {
        const val BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 1
    }
}

object WindowInsetsCompat {
    object Type {
        fun systemBars(): Int = 1
        fun navigationBars(): Int = 2
        fun statusBars(): Int = 4
    }
}

package androidx.core.content

import android.content.Context

object ContextCompat {
    fun checkSelfPermission(context: Context, permission: String): Int = 0
    fun getColor(context: Context, color: Int): Int = 0
}

package androidx.core.app

import android.app.Notification
import android.content.Context

class NotificationCompat {
    class Builder(context: Context, channelId: String) {
        fun setSmallIcon(icon: Int): Builder = this
        fun setContentTitle(title: CharSequence): Builder = this
        fun setContentText(text: CharSequence): Builder = this
        fun setPriority(priority: Int): Builder = this
        fun setAutoCancel(cancel: Boolean): Builder = this
        fun setContentIntent(intent: android.app.PendingIntent): Builder = this
        fun build(): Notification = Notification()
    }
    companion object {
        const val PRIORITY_DEFAULT = 0
        const val PRIORITY_HIGH = 1
    }
}

class NotificationManagerCompat {
    fun notify(id: Int, notification: Notification) {}
    companion object {
        fun from(context: Context): NotificationManagerCompat = NotificationManagerCompat()
    }
}
