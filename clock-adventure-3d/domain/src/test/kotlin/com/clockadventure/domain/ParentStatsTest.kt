package com.clockadventure.domain

import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.usecase.ParentStatsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parent dashboard judges a level only once the child answered a few questions, and it offers
 * a practise shortcut for every weak topic - so the level id has to survive the aggregation.
 */
class ParentStatsTest {

    private val useCase = ParentStatsUseCase()

    /** `answers` is derived from correct + wrong, so build the row from both counts. */
    private fun lesson(levelId: Int, answers: Int, correct: Int) = LessonProgress(
        levelId = levelId,
        correct = correct,
        wrong = answers - correct
    )

    @Test
    fun `levels with too few answers are never judged`() {
        val (strong, weak) = useCase.topicStats(listOf(lesson(2, answers = 2, correct = 0)))
        assertEquals(
            "two answers is not enough to call a level strong or weak",
            0,
            strong.size + weak.size
        )
    }

    @Test
    fun `a well played level is strong, a shaky one is weak`() {
        val (strong, weak) = useCase.topicStats(
            listOf(
                lesson(3, answers = 10, correct = 10),
                lesson(4, answers = 10, correct = 3)
            )
        )
        assertEquals("level 3 was answered perfectly, so it is a strength", listOf(3), strong.map { it.levelId })
        assertEquals("level 4 was answered poorly, so it needs practise", listOf(4), weak.map { it.levelId })
    }

    @Test
    fun `topics are ordered by accuracy and capped at three`() {
        val lessons = (1..5).map { lesson(it, answers = 10, correct = it) }
        val (_, weak) = useCase.topicStats(lessons)
        assertEquals("only the three weakest levels are shown", 3, weak.size)
        assertEquals("weakest first", listOf(1, 2, 3), weak.map { it.levelId })
        assertTrue(
            "accuracies are sorted ascending",
            weak.zipWithNext().all { (a, b) -> a.accuracy <= b.accuracy }
        )
    }

    @Test
    fun `every topic carries its own level id and title`() {
        val (_, weak) = useCase.topicStats(listOf(lesson(7, answers = 5, correct = 1)))
        val topic = weak.single()
        assertEquals(7, topic.levelId)
        assertTrue("the title is kept so the row can be labelled", topic.title.en.isNotBlank())
        assertEquals("1 of 5 correct is 20 percent", 20, topic.accuracyPercent)
    }
}
