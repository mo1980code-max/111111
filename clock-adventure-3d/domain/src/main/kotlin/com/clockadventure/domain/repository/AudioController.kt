package com.clockadventure.domain.repository

import com.clockadventure.domain.model.AppLanguage
import com.clockadventure.domain.model.ClockTime

/**
 * Sound and voice feedback.
 *
 * The interface lives in the domain so the learning screens can cheer the child on without
 * touching the Android audio stack; the implementation lives in the `app` module and can swap
 * TextToSpeech / AudioTrack for anything else without the UI noticing.
 */
interface AudioController {

    fun button()
    fun correct()
    fun wrong()
    fun celebrate()
    fun coin()
    fun whoosh()

    /** Speaks a time in the given language ("half past seven" / "السابعة والنصف"). */
    fun speakTime(time: ClockTime, language: AppLanguage)

    /** Speaks an arbitrary sentence, used for praise and instructions. */
    fun speak(text: String, language: AppLanguage)

    fun stopSpeaking()

    fun applySettings(musicEnabled: Boolean, soundEnabled: Boolean, voiceEnabled: Boolean)

    fun pauseMusic()

    fun resumeMusic()

    fun release()
}
