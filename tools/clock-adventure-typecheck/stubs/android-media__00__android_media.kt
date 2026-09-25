package android.media

import android.content.Context

class AudioAttributes {
    class Builder {
        fun setUsage(usage: Int): Builder = this
        fun setContentType(type: Int): Builder = this
        fun build(): AudioAttributes = AudioAttributes()
    }
    companion object {
        const val USAGE_GAME = 14
        const val CONTENT_TYPE_MUSIC = 2
    }
}

class SoundPool {
    class Builder {
        fun setMaxStreams(max: Int): Builder = this
        fun setAudioAttributes(attributes: AudioAttributes): Builder = this
        fun build(): SoundPool = SoundPool()
    }
    fun load(fd: android.content.res.AssetFileDescriptor, priority: Int): Int = 1
    fun play(soundId: Int, leftVolume: Float, rightVolume: Float, priority: Int, loop: Int, rate: Float): Int = 1
    fun release() {}
    fun stop(soundId: Int) {}
}

class MediaPlayer {
    var isLooping: Boolean = false
    val isPlaying: Boolean get() = false
    fun setDataSource(path: String) {}
    fun setDataSource(context: Context, uri: android.net.Uri) {}
    fun prepare() {}
    fun start() {}
    fun pause() {}
    fun stop() {}
    fun release() {}
    fun setVolume(left: Float, right: Float) {}
    fun setAudioAttributes(attributes: AudioAttributes) {}
}
