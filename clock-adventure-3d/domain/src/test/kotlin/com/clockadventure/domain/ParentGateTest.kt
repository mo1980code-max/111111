package com.clockadventure.domain

import com.clockadventure.domain.usecase.CreateParentGateQuestionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The gate must always be solvable and must never show two identical answers or an answer that is
 * not among the options - otherwise a parent could be locked out of their own dashboard.
 */
class ParentGateTest {

    private val useCase = CreateParentGateQuestionUseCase()

    @Test
    fun `the factors stay small enough to solve in the head`() {
        for (seed in 0 until 400) {
            val question = useCase(Random(seed))
            assertTrue("factor a = ${question.a}", question.a in 4..9)
            assertTrue("factor b = ${question.b}", question.b in 3..8)
        }
    }

    @Test
    fun `the correct answer is always one of the options`() {
        for (seed in 0 until 400) {
            val question = useCase(Random(seed))
            assertEquals("seed $seed: ${question.text()}", question.a * question.b, question.answer)
            assertTrue(
                "seed $seed: the answer ${question.answer} must be offered (${question.options})",
                question.options.contains(question.answer)
            )
        }
    }

    @Test
    fun `the four options are distinct and positive`() {
        for (seed in 0 until 400) {
            val question = useCase(Random(seed))
            assertEquals("four options", CreateParentGateQuestionUseCase.OPTIONS, question.options.size)
            assertEquals("no duplicates", question.options.size, question.options.toSet().size)
            for (option in question.options) {
                assertTrue("option $option must be positive", option > 0)
                assertTrue("option $option must stay on one screen", option <= CreateParentGateQuestionUseCase.MAX_OPTION)
            }
        }
    }

    @Test
    fun `the answer is not always in the same place`() {
        val positions = mutableSetOf<Int>()
        for (seed in 0 until 200) {
            val question = useCase(Random(seed))
            positions += question.options.indexOf(question.answer)
        }
        assertTrue("the answer must move around: $positions", positions.size > 1)
    }

    @Test
    fun `the question reads like a sum`() {
        val question = useCase(Random(1))
        assertTrue(question.text().contains("×"))
        assertTrue(question.text().endsWith("= ?"))
    }
}
