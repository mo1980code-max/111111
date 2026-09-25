package com.clockadventure.domain

import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.engine.Grader
import com.clockadventure.domain.engine.QuestionFactory
import com.clockadventure.domain.model.ClockHand
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.Question
import com.clockadventure.domain.model.QuestionKind
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Grading must never be harsh: a wrong answer always comes with a hint and a kind word. */
class GraderTest {

    private val random = Random(7)

    private fun setClockQuestion(): Question = QuestionFactory.create(
        kind = QuestionKind.SET_CLOCK,
        spec = LevelCatalog.byId(8),
        difficulty = Difficulty.MEDIUM,
        index = 3,
        random = random
    )

    @Test
    fun `an exact set clock answer is correct`() {
        val question = setClockQuestion()
        val result = Grader.grade(question, setTime = question.targetTime, random = random)
        assertTrue(result.correct)
        assertNull(result.hint)
    }

    @Test
    fun `a wrong minute is highlighted and can be retried`() {
        val question = setClockQuestion()
        val wrongMinute = ClockTime.of12(question.targetTime.hour12, (question.targetTime.minute + 15) % 60, pm = false)
        val result = Grader.grade(question, setTime = wrongMinute, random = random)
        assertFalse(result.correct)
        assertEquals(ClockHand.MINUTE, result.hint?.focusHand)
        assertFalse(result.hint!!.minuteHandCorrect)
        assertTrue(result.hint!!.hourHandCorrect)
    }

    @Test
    fun `a wrong hour is highlighted`() {
        val question = setClockQuestion()
        val wrongHour = ClockTime.of12((question.targetTime.hour12 % 12) + 1, question.targetTime.minute, pm = false)
        val result = Grader.grade(question, setTime = wrongHour, random = random)
        assertFalse(result.correct)
        assertEquals(ClockHand.HOUR, result.hint?.focusHand)
    }

    @Test
    fun `the right multiple choice answer wins`() {
        val question = QuestionFactory.create(
            kind = QuestionKind.READ_TIME,
            spec = LevelCatalog.byId(6),
            difficulty = Difficulty.MEDIUM,
            index = 1,
            random = random
        )
        val right = Grader.grade(question, choiceId = question.correctChoiceId, random = random)
        assertTrue(right.correct)
        val wrongId = question.choices.first { it.id != question.correctChoiceId }.id
        assertFalse(Grader.grade(question, choiceId = wrongId, random = random).correct)
    }

    @Test
    fun `true or false is graded against the statement`() {
        val question = QuestionFactory.create(
            kind = QuestionKind.TRUE_FALSE,
            spec = LevelCatalog.byId(10),
            difficulty = Difficulty.MEDIUM,
            index = 5,
            random = random
        )
        val expected = if (question.statementIsTrue == true) Question.CHOICE_TRUE else Question.CHOICE_FALSE
        assertTrue(Grader.grade(question, choiceId = expected, random = random).correct)
        val other = if (expected == Question.CHOICE_TRUE) Question.CHOICE_FALSE else Question.CHOICE_TRUE
        assertFalse(Grader.grade(question, choiceId = other, random = random).correct)
    }

    @Test
    fun `tap number grading uses the asked hand`() {
        val question = QuestionFactory.create(
            spec = LevelCatalog.byId(1),
            difficulty = Difficulty.EASY,
            index = 2,
            random = random
        )
        val expectedNumber = QuestionFactory.expectedTapNumber(question)
        val correctId = question.choices.first { it.number == expectedNumber }.id
        assertTrue(Grader.grade(question, choiceId = correctId, random = random).correct)
        val wrongId = question.choices.first { it.number != expectedNumber }.id
        val wrong = Grader.grade(question, choiceId = wrongId, random = random)
        assertFalse(wrong.correct)
        assertEquals(question.tapHand, wrong.hint?.focusHand)
    }

    @Test
    fun `every wrong answer carries an explanation`() {
        val question = setClockQuestion()
        val result = Grader.grade(question, setTime = ClockTime.of12(1, 0, false), random = random)
        assertTrue(result.message.en.isNotBlank())
        assertTrue(result.message.ar.isNotBlank())
        assertTrue(Grader.explanation(question).en.contains("hand"))
        assertTrue(Grader.explanation(question).ar.isNotBlank())
    }
}
