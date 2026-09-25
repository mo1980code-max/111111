package com.clockadventure.domain.catalog

import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.LocalizedText
import com.clockadventure.domain.model.QuestionKind

/**
 * The ten learning levels. They go from "which number is the hand pointing at" all the way to
 * timed mixed challenges, exactly like a real teaching plan.
 *
 * [stepEasy], [stepMedium] and [stepHard] are the minute granularities used per difficulty:
 * 60 = full hours only, 30 = half hours, 15 = quarters, 5 = five minute steps, 1 = every minute.
 */
data class LessonSpec(
    val id: Int,
    val title: LocalizedText,
    val subtitle: LocalizedText,
    /** What the mascot explains before the questions start. */
    val teach: LocalizedText,
    val kinds: List<QuestionKind>,
    val questionCount: Int = 8,
    val stepEasy: Int = 60,
    val stepMedium: Int = 60,
    val stepHard: Int = 60,
    /** While practising a new skill the digital time stays visible as a helper. */
    val showDigitalHelper: Boolean = false,
    /** Optional per question time limit in milliseconds (timed challenges). */
    val timeLimitMs: Long? = null,
    val baseDifficulty: Difficulty = Difficulty.EASY
) {
    fun minuteStepFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> stepEasy
        Difficulty.MEDIUM -> stepMedium
        Difficulty.HARD -> stepHard
    }

    /** How far the hands snap while dragging, in minutes. */
    fun snapMinutesFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.EASY -> minuteStepFor(Difficulty.EASY)
        Difficulty.MEDIUM -> minuteStepFor(Difficulty.MEDIUM)
        Difficulty.HARD -> minuteStepFor(Difficulty.HARD)
    }
}

object LevelCatalog {

    val levels: List<LessonSpec> = listOf(
        LessonSpec(
            id = 1,
            title = LocalizedText("Clock Numbers", "أرقام الساعة"),
            subtitle = LocalizedText("Meet the hands and the numbers", "تعرّف على العقارب والأرقام"),
            teach = LocalizedText(
                "The SHORT hand is the hour hand. The LONG hand is the minute hand. Let's find the numbers!",
                "العقرب القصير هو عقرب الساعات، والعقرب الطويل هو عقرب الدقائق. هيا نتعرف على الأرقام!"
            ),
            kinds = listOf(QuestionKind.TAP_NUMBER),
            questionCount = 8,
            stepEasy = 60, stepMedium = 60, stepHard = 60,
            showDigitalHelper = true,
            baseDifficulty = Difficulty.EASY
        ),
        LessonSpec(
            id = 2,
            title = LocalizedText("Full Hours", "الساعات الكاملة"),
            subtitle = LocalizedText("1:00, 2:00, 3:00 ...", "1:00، 2:00، 3:00 ..."),
            teach = LocalizedText(
                "When the long hand points at 12, it is a full hour. The short hand tells us which one!",
                "عندما يشير العقرب الطويل إلى 12 تكون الساعة كاملة، والعقرب القصير يخبرنا أي ساعة هي!"
            ),
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK),
            questionCount = 8,
            stepEasy = 60, stepMedium = 60, stepHard = 60,
            showDigitalHelper = true,
            baseDifficulty = Difficulty.EASY
        ),
        LessonSpec(
            id = 3,
            title = LocalizedText("Half Hours", "نصف الساعة"),
            subtitle = LocalizedText("2:30, 5:30 ...", "2:30، 5:30 ..."),
            teach = LocalizedText(
                "When the long hand points at 6 it is HALF past. The short hand sits between two numbers.",
                "عندما يشير العقرب الطويل إلى 6 يكون الوقت نصف الساعة، ويقف العقرب القصير بين رقمين."
            ),
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK),
            questionCount = 8,
            stepEasy = 30, stepMedium = 30, stepHard = 30,
            showDigitalHelper = true,
            baseDifficulty = Difficulty.EASY
        ),
        LessonSpec(
            id = 4,
            title = LocalizedText("Quarter Past & To", "الربع إلاّ والربع"),
            subtitle = LocalizedText("Quarter past, half, quarter to", "والربع، والنصف، إلا ربع"),
            teach = LocalizedText(
                "15 minutes is a quarter of the clock: quarter past points at 3, quarter to points at 9.",
                "خمس عشرة دقيقة هي ربع الساعة: والربع يشير إلى 3، وإلا ربع يشير إلى 9."
            ),
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK),
            questionCount = 8,
            stepEasy = 15, stepMedium = 15, stepHard = 15,
            showDigitalHelper = true,
            baseDifficulty = Difficulty.EASY
        ),
        LessonSpec(
            id = 5,
            title = LocalizedText("Five Minutes", "خمس دقائق"),
            subtitle = LocalizedText("Every 5 minutes", "كل خمس دقائق"),
            teach = LocalizedText(
                "Every number on the clock is 5 minutes. Count: 5, 10, 15, 20, 25 ...",
                "كل رقم على الساعة يساوي 5 دقائق. عد معي: 5، 10، 15، 20، 25 ..."
            ),
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK),
            questionCount = 9,
            stepEasy = 15, stepMedium = 5, stepHard = 5,
            showDigitalHelper = true,
            baseDifficulty = Difficulty.EASY
        ),
        LessonSpec(
            id = 6,
            title = LocalizedText("Read Any Time", "اقرأ أي وقت"),
            subtitle = LocalizedText("Tricky random clock faces", "أوقات عشوائية أصعب"),
            teach = LocalizedText(
                "Now the hands can be anywhere. Read the hour hand first, then count the minutes.",
                "الآن يمكن أن يكون العقربان في أي مكان. اقرأ عقرب الساعات أولاً ثم احسب الدقائق."
            ),
            kinds = listOf(QuestionKind.READ_TIME),
            questionCount = 9,
            stepEasy = 15, stepMedium = 5, stepHard = 1,
            showDigitalHelper = false,
            baseDifficulty = Difficulty.MEDIUM
        ),
        LessonSpec(
            id = 7,
            title = LocalizedText("Clock to Digital", "من الساعة إلى الرقمية"),
            subtitle = LocalizedText("Write what you read", "اكتب ما تقرأه"),
            teach = LocalizedText(
                "Look at the clock and choose the digital time: hours first, then minutes.",
                "انظر إلى الساعة واختر الوقت الرقمي: الساعات أولاً ثم الدقائق."
            ),
            kinds = listOf(QuestionKind.READ_TIME),
            questionCount = 9,
            stepEasy = 30, stepMedium = 5, stepHard = 1,
            showDigitalHelper = false,
            baseDifficulty = Difficulty.MEDIUM
        ),
        LessonSpec(
            id = 8,
            title = LocalizedText("Set the Clock", "اضبط الساعة"),
            subtitle = LocalizedText("Move the hands yourself", "حرّك العقارب بنفسك"),
            teach = LocalizedText(
                "Drag the long hand for the minutes and the short hand for the hour. They move together!",
                "اسحب العقرب الطويل للدقائق والقصير للساعات، إنهما يتحركان معاً!"
            ),
            kinds = listOf(QuestionKind.SET_CLOCK),
            questionCount = 8,
            stepEasy = 60, stepMedium = 30, stepHard = 5,
            showDigitalHelper = false,
            baseDifficulty = Difficulty.EASY
        ),
        LessonSpec(
            id = 9,
            title = LocalizedText("Real Life Time", "وقت الحياة اليومية"),
            subtitle = LocalizedText("School, lunch, bedtime ...", "المدرسة، الغداء، النوم ..."),
            teach = LocalizedText(
                "Time is everywhere in your day. Let's put your day on the clock!",
                "الوقت موجود في كل مكان في يومك. هيا نضع يومك على الساعة!"
            ),
            kinds = listOf(QuestionKind.ROUTINE_CHOICE, QuestionKind.ROUTINE_SET_CLOCK),
            questionCount = 8,
            stepEasy = 30, stepMedium = 15, stepHard = 5,
            showDigitalHelper = false,
            baseDifficulty = Difficulty.MEDIUM
        ),
        LessonSpec(
            id = 10,
            title = LocalizedText("Time Challenge", "تحدي الوقت"),
            subtitle = LocalizedText("Quick fire mixed questions", "أسئلة سريعة متنوعة"),
            teach = LocalizedText(
                "You are a clock hero now! Answer quickly - and remember, mistakes are how we learn.",
                "أنت بطل الساعة الآن! أجب بسرعة، وتذكر أن الخطأ هو طريق التعلم."
            ),
            kinds = listOf(QuestionKind.READ_TIME, QuestionKind.SET_CLOCK, QuestionKind.TRUE_FALSE),
            questionCount = 10,
            stepEasy = 30, stepMedium = 5, stepHard = 1,
            showDigitalHelper = false,
            timeLimitMs = 30_000L,
            baseDifficulty = Difficulty.MEDIUM
        )
    )

    val levelIds: List<Int> get() = levels.map { it.id }

    fun byId(id: Int): LessonSpec = levels.firstOrNull { it.id == id } ?: levels.first()

    fun nextId(id: Int): Int? = levels.firstOrNull { it.id == id + 1 }?.id

    /**
     * Highest level the child may play: every level is unlocked as soon as the previous one has at
     * least one star, and level 1 is always open.
     */
    fun unlockedThrough(lessons: List<LessonProgress>): Int {
        val done = lessons.filter { it.isCompleted }.map { it.levelId }.toSet()
        var unlocked = 1
        for (level in levels) {
            if (level.id == 1 || (level.id - 1) in done) {
                unlocked = level.id
            } else {
                break
            }
        }
        return unlocked
    }

    const val TOTAL_STARS = 30
}
