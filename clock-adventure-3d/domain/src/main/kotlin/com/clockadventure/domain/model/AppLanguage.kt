package com.clockadventure.domain.model

/**
 * Languages the app ships with. The whole app is built RTL-ready: Arabic switches the layout
 * direction, the typography (Cairo) and the spoken feedback, English uses LTR.
 */
enum class AppLanguage(val tag: String, val isRtl: Boolean) {
    ARABIC("ar", true),
    ENGLISH("en", false);

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

/**
 * How the difficulty of generated questions is chosen.
 *
 * [AUTO] keeps the child in the flow zone: the [com.clockadventure.domain.engine.DifficultyEngine]
 * tracks the last answers of the current session and moves the difficulty up or down.
 */
enum class DifficultyMode { AUTO, EASY, MEDIUM, HARD }

/**
 * A piece of text that exists in every shipped language.
 *
 * Domain content (question prompts, hints, level titles, achievements, unlockables) is produced
 * by plain Kotlin code with no access to Android resources, so it carries its own translations.
 * Chrome that never changes (button labels, screen titles) lives in `res/values` + `res/values-ar`.
 */
data class LocalizedText(val en: String, val ar: String) {
    fun text(language: AppLanguage): String = when (language) {
        AppLanguage.ARABIC -> ar
        AppLanguage.ENGLISH -> en
    }

    /** Shorthand used all over the UI: `text[language]`. */
    operator fun get(language: AppLanguage): String = text(language)

    companion object {
        fun of(en: String, ar: String) = LocalizedText(en, ar)
    }
}
