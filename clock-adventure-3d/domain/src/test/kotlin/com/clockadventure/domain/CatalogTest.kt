package com.clockadventure.domain

import com.clockadventure.domain.catalog.ChallengeCatalog
import com.clockadventure.domain.catalog.GameCatalog
import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.QuestionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The learning path itself: ten levels, their order and their unlocking rules. */
class CatalogTest {

    @Test
    fun `the learning path has exactly ten levels in order`() {
        assertEquals(10, LevelCatalog.levels.size)
        assertEquals((1..10).toList(), LevelCatalog.levels.map { it.id })
    }

    @Test
    fun `byId and nextId behave`() {
        assertEquals(1, LevelCatalog.byId(1).id)
        assertEquals(10, LevelCatalog.byId(10).id)
        // An unknown id must never crash the app: it falls back to the first level.
        assertEquals(1, LevelCatalog.byId(99).id)
        assertEquals(4, LevelCatalog.nextId(3))
        assertNull(LevelCatalog.nextId(10))
    }

    @Test
    fun `levels unlock one after another`() {
        assertEquals(1, LevelCatalog.unlockedThrough(emptyList()))
        val firstDone = listOf(LessonProgress(levelId = 1, bestStars = 1))
        assertEquals(2, LevelCatalog.unlockedThrough(firstDone))
        val threeDone = (1..3).map { LessonProgress(levelId = it, bestStars = 3) }
        assertEquals(4, LevelCatalog.unlockedThrough(threeDone))
        // A gap cannot happen, but if it did the chain must stop at the gap.
        val withGap = listOf(LessonProgress(levelId = 1, bestStars = 3), LessonProgress(levelId = 3, bestStars = 3))
        assertEquals(2, LevelCatalog.unlockedThrough(withGap))
        // Nothing completed but a high current level still keeps level 1 open.
        val everything = (1..10).map { LessonProgress(levelId = it, bestStars = 2) }
        assertEquals(10, LevelCatalog.unlockedThrough(everything))
    }

    @Test
    fun `every level can be taught`() {
        for (level in LevelCatalog.levels) {
            assertTrue("level ${level.id} needs a title", level.title.en.isNotBlank() && level.title.ar.isNotBlank())
            assertTrue("level ${level.id} needs a teach text", level.teach.en.isNotBlank() && level.teach.ar.isNotBlank())
            assertTrue("level ${level.id} needs a question kind", level.kinds.isNotEmpty())
            assertTrue("level ${level.id} needs questions", level.questionCount > 0)
            for (kind in level.kinds) {
                assertNotNull(kind)
            }
        }
    }

    @Test
    fun `difficulty steps only ever get finer`() {
        for (level in LevelCatalog.levels) {
            assertTrue(
                "level ${level.id}: easy is coarser than medium",
                level.stepEasy >= level.stepMedium
            )
            assertTrue(
                "level ${level.id}: medium is coarser than hard",
                level.stepMedium >= level.stepHard
            )
            for (difficulty in Difficulty.entries) {
                val step = level.minuteStepFor(difficulty)
                assertTrue("level ${level.id} $difficulty: step $step", step in listOf(1, 5, 15, 30, 60))
            }
        }
    }

    @Test
    fun `the ten levels cover the whole curriculum`() {
        val kinds = LevelCatalog.levels.flatMap { it.kinds }.toSet()
        for (kind in QuestionKind.entries) {
            assertTrue("the curriculum must teach $kind", kinds.contains(kind))
        }
    }

    // ------------------------------------------------------------------ challenges & games

    @Test
    fun `challenges are playable and distinct`() {
        assertEquals(4, ChallengeCatalog.items.size)
        val ids = ChallengeCatalog.items.map { it.id }.toSet()
        assertEquals(4, ids.size)
        for (challenge in ChallengeCatalog.items) {
            assertNotNull(ChallengeCatalog.byId(challenge.id))
            assertTrue(challenge.title.en.isNotBlank() && challenge.title.ar.isNotBlank())
            assertTrue(challenge.kinds.isNotEmpty())
            assertTrue(challenge.stepEasy >= challenge.stepMedium)
            assertTrue(challenge.stepMedium >= challenge.stepHard)
            if (challenge.endless) {
                assertTrue("an endless challenge needs a time limit", challenge.totalTimeLimitMs != null)
            } else {
                assertTrue("a counted challenge needs questions", challenge.questionCount > 0)
            }
        }
    }

    @Test
    fun `every mini game has a name, a kind and a difficulty curve`() {
        assertEquals("six mini games", 6, GameCatalog.games.size)
        for (game in GameCatalog.games) {
            assertTrue("game ${game.id} needs a title", game.title.en.isNotBlank() && game.title.ar.isNotBlank())
            if (game.id == com.clockadventure.domain.catalog.GameId.MATCH_TIME) {
                // Match builds its own clock/time pairs; it is the one game with no question kinds.
                assertTrue("match builds its own round", game.kinds.isEmpty())
            } else {
                assertTrue("game ${game.id} needs a question kind", game.kinds.isNotEmpty())
            }
            assertEquals("game ${game.id} lookup", game.id, GameCatalog.byId(game.id).id)
            assertTrue("game ${game.id}: easy ${game.stepEasy} >= medium ${game.stepMedium}", game.stepEasy >= game.stepMedium)
            assertTrue("game ${game.id}: medium ${game.stepMedium} >= hard ${game.stepHard}", game.stepMedium >= game.stepHard)
        }
    }
}
