package com.clockadventure.domain

import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.engine.QuestionFactory
import com.clockadventure.domain.model.ClockHand
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.QuestionKind
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Question generation: no duplicates, no impossible questions, no giveaways. */
class QuestionFactoryTest {

    private val random = Random(1234)

    @Test
    fun `distractors are never the correct time and never repeat`() {
        val correct = ClockTime.of12(7, 30, pm = false)
        val distractors = QuestionFactory.distractors(correct, 3, 5, 120, random)
        assertEquals(3, distractors.size)
        assertEquals(3, distractors.toSet().size)
        distractors.forEach { assertNotEquals(correct, it) }
    }

    @Test
    fun `level two only generates full hours`() {
        val spec = LevelCatalog.byId(2)
        repeat(60) { index ->
            val question = QuestionFactory.create(spec, Difficulty.EASY, index, random)
            assertEquals(0, question.targetTime.minute)
        }
    }

    @Test
    fun `level three generates full and half hours`() {
        val spec = LevelCatalog.byId(3)
        repeat(60) { index ->
            val question = QuestionFactory.create(spec, Difficulty.EASY, index, random)
            val minute = question.targetTime.minute
            assertTrue("unexpected minute $minute", minute == 0 || minute == 30)
        }
    }

    @Test
    fun `hard level six can generate every minute`() {
        val spec = LevelCatalog.byId(6)
        val minutes = (0 until 200).map { index ->
            QuestionFactory.create(QuestionKind.READ_TIME, spec, Difficulty.HARD, index, random).targetTime.minute
        }.toSet()
        assertTrue("expected a wide spread of minutes, got ${minutes.size}", minutes.size > 20)
    }

    @Test
    fun `multiple choice questions always contain the right answer exactly once`() {
        LevelCatalog.levels.filter { it.kinds.contains(QuestionKind.READ_TIME) }.forEach { spec ->
            repeat(30) { index ->
                val question = QuestionFactory.create(QuestionKind.READ_TIME, spec, Difficulty.MEDIUM, index, random)
                val correctChoices = question.choices.filter { it.time == question.targetTime }
                assertEquals(1, correctChoices.size)
                assertEquals(question.correctChoiceId, correctChoices.first().id)
                assertEquals(Difficulty.MEDIUM.choiceCount, question.choices.size)
            }
        }
    }

    @Test
    fun `tap number questions ask for a reachable number`() {
        val spec = LevelCatalog.byId(1)
        repeat(40) { index ->
            val question = QuestionFactory.create(spec, Difficulty.EASY, index, random)
            val expected = QuestionFactory.expectedTapNumber(question)
            assertTrue(expected in 1..12)
            assertTrue(question.choices.any { it.number == expected })
            if (question.tapHand == ClockHand.HOUR) {
                assertEquals(question.targetTime.hour12, expected)
                // The short hand has to sit exactly on the number for this exercise.
                assertEquals(0, question.targetTime.minute)
            } else {
                assertEquals(question.targetTime.minuteNumber, expected)
                assertEquals(0, question.targetTime.minute % 5)
            }
        }
    }

    @Test
    fun `true or false questions are consistent`() {
        val spec = LevelCatalog.byId(10)
        repeat(40) { index ->
            val question = QuestionFactory.create(QuestionKind.TRUE_FALSE, spec, Difficulty.MEDIUM, index, random)
            val isTrue = question.statementIsTrue!!
            assertEquals(isTrue, question.targetTime == question.shownTime)
            assertEquals(
                if (isTrue) com.clockadventure.domain.model.Question.CHOICE_TRUE else com.clockadventure.domain.model.Question.CHOICE_FALSE,
                question.correctChoiceId
            )
        }
    }

    @Test
    fun `set clock questions never leak the answer as a choice`() {
        val spec = LevelCatalog.byId(8)
        repeat(20) { index ->
            val question = QuestionFactory.create(QuestionKind.SET_CLOCK, spec, Difficulty.MEDIUM, index, random)
            assertTrue(question.choices.isEmpty())
        }
    }

    @Test
    fun `match rounds contain unique times`() {
        val round = QuestionFactory.createMatchRound(Difficulty.MEDIUM, 4, random)
        assertEquals(4, round.pairs.size)
        assertEquals(4, round.pairs.map { it.time }.toSet().size)
    }

    @Test
    fun `a routine round never repeats an activity`() {
        val round = QuestionFactory.routineRound(random, 6)
        assertEquals(6, round.size)
        assertEquals(6, round.map { it.id }.toSet().size)
    }

    @Test
    fun `generated times are always valid clock times`() {
        repeat(300) {
            val time = QuestionFactory.randomTime(5, random)
            assertTrue(time.hour in 0..23)
            assertTrue(time.minute in 0..59)
        }
    }

    @Test
    fun `the suggested level is the first unfinished one`() {
        val stars = mapOf(1 to 3, 2 to 2, 3 to 1)
        assertEquals(4, QuestionFactory.suggestedLevelId(stars))
        assertFalse(QuestionFactory.suggestedLevelId(emptyMap()) == 0)
    }
}
