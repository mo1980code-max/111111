package com.clockadventure.domain.engine

import com.clockadventure.domain.model.AnswerHint
import com.clockadventure.domain.model.AnswerResult
import com.clockadventure.domain.model.ClockHand
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.Question
import com.clockadventure.domain.model.QuestionKind
import kotlin.random.Random

/**
 * Grades an answer and - more importantly - explains it.
 *
 * A wrong answer is never a failure here: the result always carries a hint that says which hand
 * was off, so the UI can highlight exactly that hand and let the child try again.
 */
object Grader {

    private val PRAISE = listOf(
        LocalizedText("Great job!", "عمل رائع!"),
        LocalizedText("Perfect!", "ممتاز!"),
        LocalizedText("You did it!", "أنت بطل!"),
        LocalizedText("Well done!", "أحسنت!"),
        LocalizedText("Amazing!", "مذهل!"),
        LocalizedText("That's right!", "إجابة صحيحة!")
    )

    private val TRY_AGAIN = listOf(
        LocalizedText("Almost! Look at the hands.", "قريب جداً! انظر إلى العقارب."),
        LocalizedText("Nice try, let's look together.", "محاولة جميلة، هيا ننظر معاً."),
        LocalizedText("Not yet - you can fix it!", "ليس بعد - يمكنك تصحيحها!")
    )

    fun grade(
        question: Question,
        choiceId: String? = null,
        setTime: ClockTime? = null,
        random: Random = Random.Default
    ): AnswerResult = when (question.kind) {
        QuestionKind.TAP_NUMBER -> gradeTapNumber(question, choiceId, random)
        QuestionKind.SET_CLOCK, QuestionKind.ROUTINE_SET_CLOCK -> gradeSetClock(question, setTime, random)
        QuestionKind.TRUE_FALSE -> gradeTrueFalse(question, choiceId, random)
        else -> gradeChoice(question, choiceId, random)
    }

    private fun gradeTapNumber(question: Question, choiceId: String?, random: Random): AnswerResult {
        val chosen = question.choices.firstOrNull { it.id == choiceId }
        val expectedNumber = QuestionFactory.expectedTapNumber(question)
        val correct = chosen?.number == expectedNumber
        val hand = question.tapHand ?: ClockHand.HOUR
        return AnswerResult(
            correct = correct,
            expected = question.targetTime,
            given = null,
            hint = if (correct) null else AnswerHint(
                expected = question.targetTime,
                given = null,
                hourHandCorrect = hand != ClockHand.HOUR,
                minuteHandCorrect = hand != ClockHand.MINUTE,
                focusHand = hand
            ),
            message = if (correct) praise(random) else LocalizedText(
                "The ${handNameEn(hand)} hand points at $expectedNumber.",
                "عقرب ${handNameAr(hand)} يشير إلى الرقم $expectedNumber."
            )
        )
    }

    private fun gradeSetClock(question: Question, setTime: ClockTime?, random: Random): AnswerResult {
        val expected = question.targetTime
        val given = setTime
        val correct = given != null && given.hour == expected.hour && given.minute == expected.minute
        val hourOk = given != null && given.hour12 == expected.hour12
        val minuteOk = given != null && given.minute == expected.minute
        return AnswerResult(
            correct = correct,
            expected = expected,
            given = given,
            hint = if (correct || given == null) null else AnswerHint(
                expected = expected,
                given = given,
                hourHandCorrect = hourOk,
                minuteHandCorrect = minuteOk,
                focusHand = when {
                    !minuteOk -> ClockHand.MINUTE
                    !hourOk -> ClockHand.HOUR
                    else -> null
                }
            ),
            message = if (correct) praise(random) else tryAgain(random)
        )
    }

    private fun gradeTrueFalse(question: Question, choiceId: String?, random: Random): AnswerResult {
        val expectedTrue = question.statementIsTrue == true
        val answeredTrue = choiceId == Question.CHOICE_TRUE
        val correct = expectedTrue == answeredTrue
        return AnswerResult(
            correct = correct,
            expected = question.targetTime,
            given = question.shownTime,
            hint = if (correct) null else AnswerHint(
                expected = question.targetTime,
                given = question.shownTime,
                hourHandCorrect = question.targetTime.hour12 == question.shownTime.hour12,
                minuteHandCorrect = question.targetTime.minute == question.shownTime.minute,
                focusHand = null
            ),
            message = if (correct) praise(random) else tryAgain(random)
        )
    }

    private fun gradeChoice(question: Question, choiceId: String?, random: Random): AnswerResult {
        val chosen = question.choices.firstOrNull { it.id == choiceId }
        val correct = chosen?.id == question.correctChoiceId
        val given = chosen?.time
        return AnswerResult(
            correct = correct,
            expected = question.targetTime,
            given = given,
            hint = if (correct) null else AnswerHint(
                expected = question.targetTime,
                given = given,
                hourHandCorrect = given?.hour12 == question.targetTime.hour12,
                minuteHandCorrect = given?.minute == question.targetTime.minute,
                focusHand = if (given != null && given.hour12 == question.targetTime.hour12) ClockHand.MINUTE else ClockHand.HOUR
            ),
            message = if (correct) praise(random) else tryAgain(random)
        )
    }

    /** Short sentence describing the right answer, used by the explanation bubble. */
    fun explanation(question: Question): LocalizedText {
        val t = question.targetTime
        return LocalizedText(
            "The long hand points at ${t.minuteNumber} (${t.minute} minutes) and the short hand points at ${t.hour12}.",
            "العقرب الطويل يشير إلى ${t.minuteNumber} (${t.minute} دقيقة) والعقرب القصير يشير إلى ${t.hour12}."
        )
    }

    private fun praise(random: Random): LocalizedText = PRAISE.random(random)
    private fun tryAgain(random: Random): LocalizedText = TRY_AGAIN.random(random)

    private fun handNameEn(hand: ClockHand): String = when (hand) {
        ClockHand.HOUR -> "hour"
        ClockHand.MINUTE -> "minute"
    }

    private fun handNameAr(hand: ClockHand): String = when (hand) {
        ClockHand.HOUR -> "الساعات"
        ClockHand.MINUTE -> "الدقائق"
    }
}
