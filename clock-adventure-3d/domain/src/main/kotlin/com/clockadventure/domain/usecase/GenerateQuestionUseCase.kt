package com.clockadventure.domain.usecase

import com.clockadventure.domain.catalog.GameSpec
import com.clockadventure.domain.catalog.LessonSpec
import com.clockadventure.domain.engine.MatchRound
import com.clockadventure.domain.engine.QuestionFactory
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.Question
import com.clockadventure.domain.model.RoutineItem
import kotlin.random.Random
import javax.inject.Inject

/**
 * Produces the next exercise of a lesson or mini game.
 *
 * Questions are generated one at a time (never as a fixed list) so the difficulty engine can react
 * to the answer that was just given.
 */
class GenerateQuestionUseCase @Inject constructor() {

    fun forLesson(
        spec: LessonSpec,
        difficulty: Difficulty,
        index: Int,
        random: Random = Random.Default
    ): Question = QuestionFactory.create(spec, difficulty, index, random)

    fun forLesson(
        kind: com.clockadventure.domain.model.QuestionKind,
        spec: LessonSpec,
        difficulty: Difficulty,
        index: Int,
        random: Random = Random.Default
    ): Question = QuestionFactory.create(kind, spec, difficulty, index, random)

    fun forGame(
        spec: GameSpec,
        difficulty: Difficulty,
        index: Int,
        random: Random = Random.Default
    ): Question = QuestionFactory.createForGame(spec, difficulty, index, random)

    fun forRace(
        difficulty: Difficulty,
        index: Int,
        random: Random = Random.Default
    ): Question = QuestionFactory.createRaceQuestion(difficulty, index, random)

    fun matchRound(
        difficulty: Difficulty,
        pairs: Int = 4,
        random: Random = Random.Default
    ): MatchRound = QuestionFactory.createMatchRound(difficulty, pairs, random)

    fun routineRound(random: Random = Random.Default, count: Int = 6): List<RoutineItem> =
        QuestionFactory.routineRound(random, count)
}
