package com.clockadventure.domain.usecase

import com.clockadventure.domain.engine.Grader
import com.clockadventure.domain.model.AnswerResult
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.Question
import kotlin.random.Random
import javax.inject.Inject

/** Grades an answer and returns the child friendly feedback including the hint. */
class EvaluateAnswerUseCase @Inject constructor() {

    operator fun invoke(
        question: Question,
        choiceId: String? = null,
        setTime: ClockTime? = null,
        random: Random = Random.Default
    ): AnswerResult = Grader.grade(question, choiceId, setTime, random)

    /** Sentence shown in the explanation bubble after a wrong answer. */
    fun explanation(question: Question) = Grader.explanation(question)
}
