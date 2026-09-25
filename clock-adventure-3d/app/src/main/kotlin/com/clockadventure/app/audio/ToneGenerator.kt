package com.clockadventure.app.audio

import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tiny WAV synthesiser.
 *
 * The app generates its own sound effects and background loop instead of shipping audio files:
 * the APK stays small, nothing has to be downloaded and the whole game works offline.
 *
 * The output is a real 44.1 kHz / 16 bit mono WAV file, the format both SoundPool and MediaPlayer
 * accept from a file path (raw PCM would be rejected: SoundPool decodes the file, it cannot play a
 * header-less buffer).
 */
object ToneGenerator {

    private const val SAMPLE_RATE = 44_100
    private const val BITS_PER_SAMPLE = 16
    private const val CHANNELS = 1

    /** Writes a complete WAV file for the given samples. */
    fun toWav(samples: ShortArray): ByteArray {
        val dataSize = samples.size * (BITS_PER_SAMPLE / 8)
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)
        val out = ByteArrayOutputStream(44 + dataSize)
        out.writeAscii("RIFF")
        out.writeIntLittleEndian(36 + dataSize)
        out.writeAscii("WAVE")
        out.writeAscii("fmt ")
        out.writeIntLittleEndian(16)                       // PCM header size
        out.writeShortLittleEndian(1)                      // format = PCM
        out.writeShortLittleEndian(CHANNELS)
        out.writeIntLittleEndian(SAMPLE_RATE)
        out.writeIntLittleEndian(byteRate)
        out.writeShortLittleEndian((CHANNELS * BITS_PER_SAMPLE / 8)) // block align
        out.writeShortLittleEndian(BITS_PER_SAMPLE)
        out.writeAscii("data")
        out.writeIntLittleEndian(dataSize)
        for (sample in samples) {
            out.write(sample.toInt() and 0xFF)
            out.write((sample.toInt() shr 8) and 0xFF)
        }
        return out.toByteArray()
    }

    /** Renders a sequence of tones as 16 bit samples. */
    fun render(frequencies: DoubleArray, millisPerTone: Int): ShortArray {
        val samplesPerTone = (SAMPLE_RATE * millisPerTone) / 1000
        val out = ShortArray(frequencies.size * samplesPerTone)
        var index = 0
        for (frequency in frequencies) {
            for (i in 0 until samplesPerTone) {
                val t = i.toDouble() / SAMPLE_RATE
                // Short attack/release envelope keeps the tones soft instead of clicky.
                val envelope = envelope(i, samplesPerTone)
                val value = sin(2.0 * PI * frequency * t) * envelope * 0.45
                out[index++] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out
    }

    /** WAV bytes for a short sound effect. */
    fun renderEffect(frequencies: DoubleArray, millisPerTone: Int): ByteArray =
        toWav(render(frequencies, millisPerTone))

    /**
     * About eight seconds of calm background music: a slow C-major arpeggio that loops seamlessly
     * because the first and the last samples are faded in and out.
     */
    fun renderMusic(): ByteArray {
        val notes = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 392.00, 329.63)
        val samples = render(notes, 460)
        val fade = (SAMPLE_RATE * 0.15).toInt().coerceAtMost(samples.size / 4)
        for (i in 0 until fade) {
            samples[i] = (samples[i] * (i.toFloat() / fade)).toInt().toShort()
            samples[samples.size - 1 - i] = (samples[samples.size - 1 - i] * (i.toFloat() / fade)).toInt().toShort()
        }
        return toWav(samples)
    }

    private fun envelope(index: Int, total: Int): Double {
        val attack = (total * 0.08).toInt().coerceAtLeast(1)
        val release = (total * 0.3).toInt().coerceAtLeast(1)
        return when {
            index < attack -> index.toDouble() / attack
            index > total - release -> (total - index).toDouble() / release
            else -> 1.0
        }
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) {
        for (char in value) write(char.code)
    }

    private fun ByteArrayOutputStream.writeIntLittleEndian(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeShortLittleEndian(value: Int) {
        val int = value
        write(int and 0xFF)
        write((int shr 8) and 0xFF)
    }
}

/** Builds the sentence a voice speaks for a given time, in English or Arabic. */
object SpokenTime {

    fun text(
        time: com.clockadventure.domain.model.ClockTime,
        language: com.clockadventure.domain.model.AppLanguage
    ): String = com.clockadventure.domain.engine.TimeFormatter.spoken(time, language)
}
