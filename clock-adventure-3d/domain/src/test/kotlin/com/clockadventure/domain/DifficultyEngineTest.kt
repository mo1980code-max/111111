package com.clockadventure.domain

import com.clockadventure.domain.engine.DifficultyEngine
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.DifficultyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The adaptive engine: help when a child struggles, harder questions when it clicks. */
class DifficultyEngineTest {

    @Test
    fun `three correct answers make it harder`() {
        val engine = DifficultyEngine(DifficultyMode.AUTO, Difficulty.EASY)
        assertEquals(Difficulty.EASY, engine.current())
        engine.record(true)
        engine.record(true)
        assertEquals(Difficulty.EASY, engine.current())
        engine.record(true)
        assertEquals(Difficulty.MEDIUM, engine.current())
    }

    @Test
    fun `it never gets harder than hard`() {
        val engine = DifficultyEngine(DifficultyMode.AUTO, Difficulty.HARD)
        repeat(12) { engine.record(true) }
        assertEquals(Difficulty.HARD, engine.current())
    }

    @Test
    fun `two wrong answers make it easier again`() {
        val engine = DifficultyEngine(DifficultyMode.AUTO, Difficulty.HARD)
        engine.record(false)
        engine.record(false)
        assertEquals(Difficulty.MEDIUM, engine.current())
        engine.record(false)
        engine.record(false)
        assertEquals(Difficulty.EASY, engine.current())
    }

    @Test
    fun `hints switch on after a mistake and off after a good run`() {
        val engine = DifficultyEngine(DifficultyMode.AUTO, Difficulty.MEDIUM)
        assertFalse(engine.hintsOn)
        engine.record(false)
        assertTrue(engine.hintsOn)
        engine.record(true)
        assertFalse(engine.hintsOn)
    }

    @Test
    fun `a fixed mode never moves`() {
        val engine = DifficultyEngine(DifficultyMode.EASY)
        repeat(10) { engine.record(true) }
        assertEquals(Difficulty.EASY, engine.current())
        val hard = DifficultyEngine(DifficultyMode.HARD)
        repeat(10) { hard.record(false) }
        assertEquals(Difficulty.HARD, hard.current())
    }

    @Test
    fun `easy difficulty offers the most extra practice`() {
        val engine = DifficultyEngine(DifficultyMode.EASY)
        assertEquals(2, engine.practiceCount())
    }
}
