package com.clockadventure.app.audio

import kotlin.math.PI
import kotlin.math.sin

/**
 * Tiny 8 bit / mono PCM synthesiser.
 *
 * The app generates its own sound effects and background loop instead of shipping audio files:
 * the APK stays small, nothing has to be downloaded and the whole game works offline.
 * Output is unsigned 8 bit PCM at 8 kHz, the format SoundPool and MediaPlayer both accept from a
 * raw file descriptor.
 */
object ToneGenerator {

    private const val SAMPLE_RATE = 8000

    /** Renders a sequence of tones into a raw PCM byte array. */
    fun render(frequencies: DoubleArray, millisPerTone: Int): ByteArray {
        val samplesPerTone = (SAMPLE_RATE * millisPerTone) / 1000
        val out = ByteArray(frequencies.size * samplesPerTone)
        var index = 0
        for (frequency in frequencies) {
            for (i in 0 until samplesPerTone) {
                val t = i.toDouble() / SAMPLE_RATE
                // Short attack/release envelope keeps the tones soft instead of clicky.
                val envelope = envelope(i, samplesPerTone)
                val value = sin(2.0 * PI * frequency * t) * envelope * 0.6
                out[index++] = ((value * 127.0) + 128.0).toInt().coerceIn(0, 255).toByte()
            }
        }
        return out
    }

    /**
     * Eight seconds of calm background music: a slow C-major arpeggio that loops seamlessly.
     */
    fun renderMusic(): ByteArray {
        val notes = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 392.00, 329.63)
        val out = render(notes, 420)
        // Fade the last 10 % out and the first 10 % in so the loop has no audible click.
        val fade = out.size / 10
        for (i in 0 until fade) {
            out[i] = (128 + ((out[i].toInt() - 128) * i / fade)).toByte()
            out[out.size - 1 - i] = (128 + ((out[out.size - 1 - i].toInt() - 128) * i / fade)).toByte()
        }
        return out
    }

    private fun envelope(index: Int, total: Int): Double {
        val attack = (total * 0.1).toInt().coerceAtLeast(1)
        val release = (total * 0.25).toInt().coerceAtLeast(1)
        return when {
            index < attack -> index.toDouble() / attack
            index > total - release -> (total - index).toDouble() / release
            else -> 1.0
        }
    }
}

/** Builds the sentence a voice speaks for a given time, in English or Arabic. */
object SpokenTime {

    fun text(time: com.clockadventure.domain.model.ClockTime, language: com.clockadventure.domain.model.AppLanguage): String =
        com.clockadventure.domain.engine.TimeFormatter.spoken(time, language)
}
