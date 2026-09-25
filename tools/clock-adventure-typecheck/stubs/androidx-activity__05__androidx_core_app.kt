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
