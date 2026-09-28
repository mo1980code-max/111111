package android.speech.tts

import android.content.Context

class TextToSpeech(context: Context, listener: (Int) -> Unit) {
    var language: java.util.Locale
        get() = java.util.Locale.getDefault()
        set(value) {}
    fun speak(text: CharSequence, queueMode: Int, params: android.os.Bundle?, utteranceId: String): Int = 0
    fun stop(): Int = 0
    fun shutdown() {}
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener): Int = 0

    companion object {
        const val SUCCESS = 0
        const val QUEUE_FLUSH = 0
        const val QUEUE_ADD = 1
    }
}

abstract class UtteranceProgressListener {
    abstract fun onStart(utteranceId: String?)
    abstract fun onDone(utteranceId: String?)
    abstract fun onError(utteranceId: String?)
}
