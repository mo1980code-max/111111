package com.clockadventure.domain

import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.engine.QuestionFactory
import com.clockadventure.domain.model.AnswerChoice
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.Question
import com.clockadventure.domain.model.QuestionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Property style checks over the whole question catalogue.
 *
 * A child must never meet a question that is impossible to answer with the hands the level allows,
 * a choice list without the right answer, or an empty prompt - so these invariants are asserted for
 * every level, every difficulty and every question kind, over many seeded random rounds.
 */
class QuestionFactoryInvariantsTest {

    @Test
    fun `every level and difficulty produces answerable questions`() {
        for (spec in LevelCatalog.levels) {
            for (difficulty in Difficulty.entries) {
                val random = Random(spec.id * 31 + difficulty.ordinal)
                val step = spec.minuteStepFor(difficulty)
                for (index in 0 until 40) {
                    val question = QuestionFactory.create(spec, difficulty, index, random)
                    assertSane(question, step)
                }
            }
        }
    }

    @Test
    fun `every question kind is generated with the right shape`() {
        val random = Random(7)
        val spec = LevelCatalog.byId(10)
        for (kind in QuestionKind.entries) {
            val question = QuestionFactory.create(kind, spec, Difficulty.MEDIUM, 3, random)
            assertEquals(kind, question.kind)
            assertSane(question, spec.minuteStepFor(Difficulty.MEDIUM))
            when (kind) {
                QuestionKind.TAP_NUMBER -> assertNotNull(question.tapHand)
                QuestionKind.TRUE_FALSE -> assertNotNull(question.statementIsTrue)
                QuestionKind.ROUTINE_CHOICE, QuestionKind.ROUTINE_SET_CLOCK ->
                    assertNotNull(question.routine)
                QuestionKind.READ_TIME, QuestionKind.SET_CLOCK -> Unit
            }
        }
    }

    @Test
    fun `minutes obey the granularity of the difficulty`() {
        for (spec in LevelCatalog.levels) {
            for (difficulty in Difficulty.entries) {
                val step = spec.minuteStepFor(difficulty)
                val random = Random(spec.id * 101 + difficulty.ordinal)
                repeat(60) {
                    val time = QuestionFactory.randomTime(minuteStep = step, random = random)
                    assertTrue(
                        "level ${spec.id} / $difficulty: minute ${time.minute} must be a multiple of $step",
                        time.minute % step == 0
                    )
                    assertTrue(time.hour in 0..23)
                    assertTrue(time.minute in 0..59)
                }
            }
        }
    }

    @Test
    fun `every choice list contains the correct answer and no duplicates`() {
        for (spec in LevelCatalog.levels) {
            val random = Random(spec.id)
            repeat(30) { index ->
                val question = QuestionFactory.create(spec, Difficulty.EASY, index, random)
                if (question.choices.isNotEmpty()) {
                    val ids = question.choices.map { it.id }
                    assertEquals("duplicate choice ids", ids.size, ids.toSet().size)
                    assertNotNull(question.correctChoiceId)
                    assertTrue(
                        "the correct answer must be one of the choices",
                        question.choices.any { it.id == question.correctChoiceId }
                    )
                    for (choice in question.choices) {
                        assertChoiceIsRenderable(choice)
                    }
                }
            }
        }
    }

    @Test
    fun `prompts are never empty in either language`() {
        for (spec in LevelCatalog.levels) {
            val random = Random(spec.id + 5)
            repeat(10) { index ->
                val question = QuestionFactory.create(spec, Difficulty.HARD, index, random)
                assertTrue(question.prompt.en.isNotBlank())
                assertTrue(question.prompt.ar.isNotBlank())
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun assertSane(question: Question, step: Int) {
        assertTrue("question id", question.id.isNotBlank())
        assertTrue("hour", question.targetTime.hour in 0..23)
        assertTrue("minute", question.targetTime.minute in 0..59)
        assertTrue(question.prompt.en.isNotBlank())
        assertTrue(question.prompt.ar.isNotBlank())

        val isFreeFormTap = question.kind == QuestionKind.TAP_NUMBER
        val isRoutine = question.routine != null
        if (!isFreeFormTap && !isRoutine) {
            assertTrue(
                "level ${question.levelId} ${question.kind}: minute ${question.targetTime.minute} " +
                    "must be a multiple of $step",
                question.targetTime.minute % step == 0
            )
        }
        when (question.kind) {
            QuestionKind.READ_TIME ->
                assertTrue("a read-the-clock question needs choices", question.choices.size >= 2)
            QuestionKind.SET_CLOCK ->
                assertTrue(
                    "a set-the-clock question is answered with the hands, not with choices",
                    question.choices.isEmpty()
                )
            else -> Unit
        }
    }

    private fun assertChoiceIsRenderable(choice: AnswerChoice) {
        val hasTime = choice.time != null
        val hasText = choice.text != null
        val hasNumber = choice.number != null
        assertTrue("a choice needs a time, a number or a text", hasTime || hasText || hasNumber)
        if (hasText) {
            assertTrue(choice.text!!.en.isNotBlank())
            assertTrue(choice.text!!.ar.isNotBlank())
        }
        if (hasTime) {
            val time: ClockTime = choice.time!!
            assertFalse(time.hour !in 0..23)
            assertFalse(time.minute !in 0..59)
        }
    }
}
