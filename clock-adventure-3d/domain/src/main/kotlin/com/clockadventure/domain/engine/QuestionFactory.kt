package com.clockadventure.domain.engine

import com.clockadventure.domain.catalog.GameId
import com.clockadventure.domain.catalog.GameSpec
import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.catalog.LessonSpec
import com.clockadventure.domain.model.AnswerChoice
import com.clockadventure.domain.model.ClockHand
import com.clockadventure.domain.model.ClockTime
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.Question
import com.clockadventure.domain.model.RoutineCatalog
import com.clockadventure.domain.model.RoutineItem
import com.clockadventure.domain.model.QuestionKind
import kotlin.math.abs
import kotlin.random.Random

/** A pair of the Match The Time mini game. */
data class MatchPair(val id: String, val time: ClockTime)

data class MatchRound(val pairs: List<MatchPair>)

/**
 * Builds every exercise of the app.
 *
 * The factory is pure: the same seed produces the same questions, which is what the unit tests
 * rely on, and the UI never has to know how a level is assembled.
 */
object QuestionFactory {

    private val CHOICE_IDS = listOf("a", "b", "c", "d", "e")

    private val PROMPT_READ = LocalizedText("What time does the clock show?", "كم الساعة في هذه الساعة؟")
    private val PROMPT_SET = LocalizedText("Move the hands to this time", "حرّك العقارب إلى هذا الوقت")
    private val PROMPT_TRUE_FALSE = LocalizedText("Does the clock match the time?", "هل الساعة تطابق الوقت؟")
    private val PROMPT_TAP_HOUR = LocalizedText(
        "Tap the number the SHORT hour hand points to",
        "اضغط على الرقم الذي يشير إليه عقرب الساعات القصير"
    )
    private val PROMPT_TAP_MINUTE = LocalizedText(
        "Tap the number the LONG minute hand points to",
        "اضغط على الرقم الذي يشير إليه عقرب الدقائق الطويل"
    )

    // ------------------------------------------------------------------ lessons

    fun create(
        spec: LessonSpec,
        difficulty: Difficulty,
        index: Int,
        random: Random = Random.Default
    ): Question {
        val kind = spec.kinds[index % spec.kinds.size]
        return create(kind, spec, difficulty, index, random)
    }

    fun create(
        kind: QuestionKind,
        spec: LessonSpec,
        difficulty: Difficulty,
        index: Int,
        random: Random = Random.Default
    ): Question = build(
        kind = kind,
        levelId = spec.id,
        difficulty = difficulty,
        minuteStep = spec.minuteStepFor(difficulty),
        showDigitalHelper = spec.showDigitalHelper,
        index = index,
        random = random
    )

    // -------------------------------------------------------------------- games

    fun createForGame(
        spec: GameSpec,
        difficulty: Difficulty,
        index: Int,
        random: Random = Random.Default
    ): Question {
        val kind = spec.kinds.random(random)
        return build(
            kind = kind,
            levelId = 0,
            difficulty = difficulty,
            minuteStep = spec.minuteStepFor(difficulty),
            showDigitalHelper = false,
            index = index,
            random = random
        )
    }

    /** Endless rounds (Time Race) call this until the clock runs out. */
    fun createRaceQuestion(difficulty: Difficulty, index: Int, random: Random = Random.Default): Question {
        val kind = when (index % 3) {
            0 -> QuestionKind.READ_TIME
            1 -> QuestionKind.SET_CLOCK
            else -> QuestionKind.TRUE_FALSE
        }
        return build(
            kind = kind,
            levelId = 0,
            difficulty = difficulty,
            minuteStep = when (difficulty) {
                Difficulty.EASY -> 30
                Difficulty.MEDIUM -> 15
                Difficulty.HARD -> 5
            },
            showDigitalHelper = false,
            index = index,
            random = random
        )
    }

    fun createMatchRound(
        difficulty: Difficulty,
        pairs: Int = 4,
        random: Random = Random.Default
    ): MatchRound {
        val step = when (difficulty) {
            Difficulty.EASY -> 30
            Difficulty.MEDIUM -> 15
            Difficulty.HARD -> 5
        }
        val times = linkedSetOf<ClockTime>()
        var guard = 0
        while (times.size < pairs && guard++ < 200) {
            times += randomTime(step, random)
        }
        return MatchRound(times.mapIndexed { i, time -> MatchPair(id = "p$i", time = time) })
    }

    /** Six daily routine moments, shuffled, for the My Daily Routine game. */
    fun routineRound(random: Random = Random.Default, count: Int = 6): List<RoutineItem> =
        RoutineCatalog.items.shuffled(random).take(count.coerceAtMost(RoutineCatalog.items.size))

    // ------------------------------------------------------------------- builder

    private fun build(
        kind: QuestionKind,
        levelId: Int,
        difficulty: Difficulty,
        minuteStep: Int,
        showDigitalHelper: Boolean,
        index: Int,
        random: Random
    ): Question {
        val id = "$levelId-${kind.name}-$index-${random.nextInt(1_000_000)}"
        return when (kind) {
            QuestionKind.TAP_NUMBER -> tapNumber(id, levelId, difficulty, random)
            QuestionKind.READ_TIME -> readTime(id, levelId, difficulty, minuteStep, showDigitalHelper, random)
            QuestionKind.SET_CLOCK -> setClock(id, levelId, difficulty, minuteStep, random)
            QuestionKind.TRUE_FALSE -> trueFalse(id, levelId, difficulty, minuteStep, random)
            QuestionKind.ROUTINE_CHOICE -> routineChoice(id, levelId, difficulty, random)
            QuestionKind.ROUTINE_SET_CLOCK -> routineSetClock(id, levelId, random)
        }
    }

    private fun tapNumber(id: String, levelId: Int, difficulty: Difficulty, random: Random): Question {
        val hand = if (random.nextBoolean()) ClockHand.HOUR else ClockHand.MINUTE
        val time = if (hand == ClockHand.HOUR) {
            // Full hour so the short hand sits exactly on a number.
            ClockTime.of12(random.nextInt(12) + 1, 0, pm = false)
        } else {
            // A multiple of five so the long hand sits exactly on a number too.
            ClockTime.of12(random.nextInt(12) + 1, random.nextInt(12) * 5, pm = false)
        }
        val correctNumber = if (hand == ClockHand.HOUR) time.hour12 else time.minuteNumber
        val wrongNumbers = (1..12)
            .filter { it != correctNumber }
            .shuffled(random)
            .take(difficulty.choiceCount - 1)

        val choices = (wrongNumbers + correctNumber)
            .shuffled(random)
            .map { number -> AnswerChoice(id = "n$number", number = number) }

        return Question(
            id = id,
            levelId = levelId,
            kind = QuestionKind.TAP_NUMBER,
            difficulty = difficulty,
            targetTime = time,
            choices = choices,
            correctChoiceId = "n$correctNumber",
            prompt = if (hand == ClockHand.HOUR) PROMPT_TAP_HOUR else PROMPT_TAP_MINUTE,
            tapHand = hand,
            showDigitalHelper = false
        )
    }

    private fun readTime(
        id: String,
        levelId: Int,
        difficulty: Difficulty,
        minuteStep: Int,
        showDigitalHelper: Boolean,
        random: Random
    ): Question {
        val time = randomTime(minuteStep, random)
        val choices = timeChoices(time, difficulty, minuteStep, random)
        return Question(
            id = id,
            levelId = levelId,
            kind = QuestionKind.READ_TIME,
            difficulty = difficulty,
            targetTime = time,
            choices = choices,
            correctChoiceId = choices.first { it.time == time }.id,
            prompt = PROMPT_READ,
            showDigitalHelper = showDigitalHelper
        )
    }

    private fun setClock(
        id: String,
        levelId: Int,
        difficulty: Difficulty,
        minuteStep: Int,
        random: Random
    ): Question {
        val time = randomTime(minuteStep, random)
        return Question(
            id = id,
            levelId = levelId,
            kind = QuestionKind.SET_CLOCK,
            difficulty = difficulty,
            targetTime = time,
            prompt = PROMPT_SET
        )
    }

    private fun trueFalse(
        id: String,
        levelId: Int,
        difficulty: Difficulty,
        minuteStep: Int,
        random: Random
    ): Question {
        val clockTime = randomTime(minuteStep, random)
        val isTrue = random.nextBoolean()
        val statementTime = if (isTrue) {
            clockTime
        } else {
            distractors(clockTime, 1, minuteStep, difficulty.distractorSpread, random).first()
        }
        return Question(
            id = id,
            levelId = levelId,
            kind = QuestionKind.TRUE_FALSE,
            difficulty = difficulty,
            targetTime = clockTime,
            shownTime = statementTime,
            statementIsTrue = isTrue,
            choices = Question.trueFalseChoices(),
            correctChoiceId = if (isTrue) Question.CHOICE_TRUE else Question.CHOICE_FALSE,
            prompt = PROMPT_TRUE_FALSE
        )
    }

    private fun routineChoice(id: String, levelId: Int, difficulty: Difficulty, random: Random): Question {
        val routine = RoutineCatalog.items.random(random)
        val choices = timeChoices(routine.time, difficulty, 30, random)
        return Question(
            id = id,
            levelId = levelId,
            kind = QuestionKind.ROUTINE_CHOICE,
            difficulty = difficulty,
            targetTime = routine.time,
            choices = choices,
            correctChoiceId = choices.first { it.time == routine.time }.id,
            routine = routine,
            prompt = LocalizedText(
                "${routine.sentence.en} What time is it?",
                "${routine.sentence.ar} كم الساعة؟"
            )
        )
    }

    private fun routineSetClock(id: String, levelId: Int, random: Random): Question {
        val routine = RoutineCatalog.items.random(random)
        return Question(
            id = id,
            levelId = levelId,
            kind = QuestionKind.ROUTINE_SET_CLOCK,
            difficulty = Difficulty.EASY,
            targetTime = routine.time,
            routine = routine,
            prompt = LocalizedText(
                "${routine.sentence.en} Set the clock!",
                "${routine.sentence.ar} اضبط الساعة!"
            )
        )
    }

    // ------------------------------------------------------------------ helpers

    /** A random time at the given minute granularity. */
    fun randomTime(minuteStep: Int, random: Random = Random.Default): ClockTime {
        val step = minuteStep.coerceIn(1, 60)
        val buckets = (60 / step).coerceAtLeast(1)
        val minute = random.nextInt(buckets) * step
        val hour12 = random.nextInt(12) + 1
        return ClockTime.of12(hour12, minute.coerceAtMost(59), pm = false)
    }

    private fun timeChoices(
        correct: ClockTime,
        difficulty: Difficulty,
        minuteStep: Int,
        random: Random
    ): List<AnswerChoice> {
        val wrong = distractors(correct, difficulty.choiceCount - 1, minuteStep, difficulty.distractorSpread, random)
        val all = (wrong + correct).shuffled(random)
        return all.mapIndexed { index, time ->
            AnswerChoice(id = CHOICE_IDS.getOrElse(index) { "opt$index" }, time = time)
        }
    }

    /**
     * Wrong answers that are close enough to be interesting but never equal to the right time.
     * Easy levels keep them far away (a different hour), hard levels put them a few minutes off.
     */
    fun distractors(
        correct: ClockTime,
        count: Int,
        minuteStep: Int,
        spreadMinutes: Int,
        random: Random
    ): List<ClockTime> {
        val result = LinkedHashSet<ClockTime>()
        val step = minuteStep.coerceIn(1, 60)
        val spread = spreadMinutes.coerceAtLeast(step)

        val offsets = mutableListOf<Int>()
        var k = 1
        while (offsets.size < 60 && k <= 24) {
            val offset = k * step
            if (offset <= spread) {
                offsets += offset
                offsets += -offset
            }
            k++
        }
        // A different hour is always an acceptable distractor, even when the step is coarse.
        offsets += 60
        offsets += -60
        offsets += 120
        offsets += -120

        for (offset in offsets.shuffled(random)) {
            if (result.size >= count) break
            val candidate = ClockTime.fromTotalMinutes(correct.totalMinutes + offset)
            if (candidate != correct) result += candidate
        }

        var guard = 0
        while (result.size < count && guard++ < 100) {
            val candidate = randomTime(step, random)
            if (candidate != correct) result += candidate
        }
        return result.toList()
    }

    /** The number the highlighted hand should point at for a TAP_NUMBER question. */
    fun expectedTapNumber(question: Question): Int = when (question.tapHand) {
        ClockHand.MINUTE -> question.targetTime.minuteNumber
        else -> question.targetTime.hour12
    }

    /** Convenience for the "Play" button: the level the child should work on next. */
    fun suggestedLevelId(progressStarsByLevel: Map<Int, Int>): Int {
        val nextUnfinished = LevelCatalog.levels.firstOrNull { (progressStarsByLevel[it.id] ?: 0) == 0 }
        return nextUnfinished?.id ?: LevelCatalog.levels.last().id
    }
}
