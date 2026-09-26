package com.clock.livewallpaper.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Copy and share, done locally. Nothing leaves the device unless the user picks a target app
 * from the system chooser.
 */
object Sharing {

    /**
     * Puts [text] on the clipboard.
     *
     * @return true when the caller should show its own confirmation. Android 13+ shows a system
     * clipboard confirmation of its own, so duplicating it would be noise.
     */
    fun copy(context: Context, text: String, label: String): Boolean {
        val manager = context.getSystemService<ClipboardManager>() ?: return false
        return runCatching {
            manager.setPrimaryClip(ClipData.newPlainText(label, text))
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        }.getOrDefault(false)
    }

    /** Opens the system share sheet with plain text. */
    fun share(context: Context, text: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, chooserTitle).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }
}
