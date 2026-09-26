package com.clockadventure.domain.model

/**
 * Difficulty of the questions generated for a level or mini game.
 *
 * It controls three things: how coarse the minute snapping is, how close the wrong answers are
 * allowed to be to the right one, and how many choices the child has to pick from.
 */
enum class Difficulty(
    val choiceCount: Int,
    /** Minutes between two possible clock positions. */
    val minuteStep: Int,
    /** How far (in minutes) a distractor may sit from the correct time. */
    val distractorSpread: Int,
    /** Number of extra practice questions inserted after a wrong answer. */
    val practiceBoost: Int
) {
    EASY(choiceCount = 3, minuteStep = 60, distractorSpread = 120, practiceBoost = 2),
    MEDIUM(choiceCount = 3, minuteStep = 30, distractorSpread = 60, practiceBoost = 1),
    HARD(choiceCount = 4, minuteStep = 5, distractorSpread = 15, practiceBoost = 0);

    companion object {
        fun fromMode(mode: DifficultyMode, fallback: Difficulty = EASY): Difficulty = when (mode) {
            DifficultyMode.AUTO -> fallback
            DifficultyMode.EASY -> EASY
            DifficultyMode.MEDIUM -> MEDIUM
            DifficultyMode.HARD -> HARD
        }
    }
}
