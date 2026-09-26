package com.clockadventure.domain.engine

import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.DifficultyMode

/**
 * Keeps the child in the flow zone.
 *
 * Three correct answers in a row make the questions harder, two wrong answers in a row make them
 * easier again and switch the hints back on. When the parent (or the child) picks a fixed mode the
 * engine simply stays there.
 */
class DifficultyEngine(
    private val mode: DifficultyMode,
    start: Difficulty = Difficulty.EASY
) {

    private var current: Difficulty = when (mode) {
        DifficultyMode.AUTO -> start
        DifficultyMode.EASY -> Difficulty.EASY
        DifficultyMode.MEDIUM -> Difficulty.MEDIUM
        DifficultyMode.HARD -> Difficulty.HARD
    }

    private var correctStreak = 0
    private var wrongStreak = 0
    private var answers = 0

    /** True while the child is struggling: the UI then offers extra visual help. */
    var hintsOn: Boolean = false
        private set

    fun current(): Difficulty = current

    fun record(correct: Boolean): Difficulty {
        answers++
        if (correct) {
            correctStreak++
            wrongStreak = 0
        } else {
            wrongStreak++
            correctStreak = 0
        }
        hintsOn = wrongStreak >= 1

        when (mode) {
            DifficultyMode.AUTO -> {
                if (correctStreak >= 3) {
                    current = stepUp(current)
                    correctStreak = 0
                } else if (wrongStreak >= 2) {
                    current = stepDown(current)
                    wrongStreak = 0
                }
            }
            else -> Unit
        }
        return current
    }

    /** Extra practice questions appended after a mistake, so the skill is repeated. */
    fun practiceCount(): Int = current.practiceBoost

    fun answerCount(): Int = answers

    private fun stepUp(difficulty: Difficulty): Difficulty = when (difficulty) {
        Difficulty.EASY -> Difficulty.MEDIUM
        Difficulty.MEDIUM -> Difficulty.HARD
        Difficulty.HARD -> Difficulty.HARD
    }

    private fun stepDown(difficulty: Difficulty): Difficulty = when (difficulty) {
        Difficulty.HARD -> Difficulty.MEDIUM
        Difficulty.MEDIUM -> Difficulty.EASY
        Difficulty.EASY -> Difficulty.EASY
    }
}
