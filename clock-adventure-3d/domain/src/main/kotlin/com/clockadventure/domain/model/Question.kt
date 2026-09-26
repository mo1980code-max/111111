package com.clockadventure.domain.model

/** The different ways a question can be presented and answered. */
enum class QuestionKind {
    /** "Which number is the hour hand pointing at?" - pure number recognition. */
    TAP_NUMBER,

    /** A clock is shown, the child picks the matching digital time. */
    READ_TIME,

    /** A digital time is shown, the child drags the hands to match it. */
    SET_CLOCK,

    /** A clock and a time are shown, the child answers whether they match. */
    TRUE_FALSE,

    /** A real life activity is shown, the child picks the time it happens. */
    ROUTINE_CHOICE,

    /** A real life activity is shown, the child sets the clock to when it happens. */
    ROUTINE_SET_CLOCK
}

/** Which hand has to be tapped: the long minute hand or the short hour hand. */
enum class ClockHand { HOUR, MINUTE }

/** One tappable answer of a multiple choice question. */
data class AnswerChoice(
    val id: String,
    val time: ClockTime? = null,
    val number: Int? = null,
    val text: LocalizedText? = null
)

/** A wrong answer explanation pointing at the hand(s) that were set incorrectly. */
data class AnswerHint(
    val expected: ClockTime,
    val given: ClockTime?,
    val hourHandCorrect: Boolean,
    val minuteHandCorrect: Boolean,
    val focusHand: ClockHand?
)

data class AnswerResult(
    val correct: Boolean,
    val expected: ClockTime,
    val given: ClockTime?,
    val hint: AnswerHint?,
    val message: LocalizedText
)

/**
 * A single exercise. Everything the UI needs is here - no screen has to know how questions are
 * generated.
 */
data class Question(
    val id: String,
    val levelId: Int,
    val kind: QuestionKind,
    val difficulty: Difficulty,
    /** The time the child has to read, match or set. */
    val targetTime: ClockTime,
    /** For TRUE_FALSE this is the time written under the clock (may differ from [targetTime]). */
    val shownTime: ClockTime = targetTime,
    val choices: List<AnswerChoice> = emptyList(),
    val correctChoiceId: String? = null,
    val statementIsTrue: Boolean? = null,
    val routine: RoutineItem? = null,
    val prompt: LocalizedText,
    val tapHand: ClockHand? = null,
    val showDigitalHelper: Boolean = false
) {
    /** TRUE/FALSE questions are answered with these two choice ids. */
    companion object {
        const val CHOICE_TRUE = "true"
        const val CHOICE_FALSE = "false"

        fun trueFalseChoices(): List<AnswerChoice> = listOf(
            AnswerChoice(id = CHOICE_TRUE, text = LocalizedText("Yes!", "نعم!")),
            AnswerChoice(id = CHOICE_FALSE, text = LocalizedText("No!", "لا!"))
        )
    }
}
