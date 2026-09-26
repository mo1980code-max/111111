package com.clockadventure.domain.model

/** Canvas drawn badge art, so the app ships without any raster badge assets. */
enum class AchievementIcon { STAR, COIN, CLOCK, FLAWLESS, STREAK, LEVEL, ROUTINE, SPEED, GRADUATE }

/** What an achievement counts: stars, coins, answers, sessions or time. */
enum class AchievementMetric { STARS, COINS, CORRECT_ANSWERS, LESSONS_COMPLETED, FLAWLESS_LESSONS, DAYS_PLAYED, ROUTINE_ANSWERS, FAST_ANSWERS, PLAYER_LEVEL }

data class Achievement(
    val id: String,
    val title: LocalizedText,
    val description: LocalizedText,
    val icon: AchievementIcon,
    val metric: AchievementMetric,
    val goal: Int
)

data class AchievementState(
    val achievement: Achievement,
    val progress: Int,
    val unlocked: Boolean
) {
    val progressFraction: Float
        get() = (progress.toFloat() / achievement.goal.toFloat()).coerceIn(0f, 1f)
}

object AchievementCatalog {
    private fun a(
        id: String,
        icon: AchievementIcon,
        metric: AchievementMetric,
        goal: Int,
        enTitle: String,
        arTitle: String,
        enDesc: String,
        arDesc: String
    ) = Achievement(
        id = id,
        icon = icon,
        metric = metric,
        goal = goal,
        title = LocalizedText(enTitle, arTitle),
        description = LocalizedText(enDesc, arDesc)
    )

    val items: List<Achievement> = listOf(
        a("first_star", AchievementIcon.STAR, AchievementMetric.STARS, 1,
            "First Star", "أول نجمة",
            "Earn your very first star", "احصل على نجمتك الأولى"),
        a("star_collector", AchievementIcon.STAR, AchievementMetric.STARS, 15,
            "Star Collector", "جامع النجوم",
            "Collect 15 shiny stars", "اجمع 15 نجمة لامعة"),
        a("star_master", AchievementIcon.GRADUATE, AchievementMetric.STARS, 40,
            "Star Master", "سيد النجوم",
            "Collect 40 shiny stars", "اجمع 40 نجمة لامعة"),
        a("coin_hunter", AchievementIcon.COIN, AchievementMetric.COINS, 200,
            "Coin Hunter", "صائد العملات",
            "Collect 200 coins", "اجمع 200 عملة"),
        a("answer_20", AchievementIcon.CLOCK, AchievementMetric.CORRECT_ANSWERS, 20,
            "Getting Good", "أصبحت ماهراً",
            "Answer 20 questions correctly", "أجب عن 20 سؤالاً بشكل صحيح"),
        a("answer_100", AchievementIcon.CLOCK, AchievementMetric.CORRECT_ANSWERS, 100,
            "Time Expert", "خبير الوقت",
            "Answer 100 questions correctly", "أجب عن 100 سؤال بشكل صحيح"),
        a("lessons_5", AchievementIcon.LEVEL, AchievementMetric.LESSONS_COMPLETED, 5,
            "Half Way There", "في منتصف الطريق",
            "Finish 5 lessons", "أنهِ 5 دروس"),
        a("all_lessons", AchievementIcon.GRADUATE, AchievementMetric.LESSONS_COMPLETED, 10,
            "Clock Champion", "بطل الساعة",
            "Finish all 10 lessons", "أنهِ كل الدروس العشرة"),
        a("flawless", AchievementIcon.FLAWLESS, AchievementMetric.FLAWLESS_LESSONS, 3,
            "Perfect!", "مثالي!",
            "Finish 3 lessons without a mistake", "أنهِ 3 دروس دون أي خطأ"),
        a("daily_3", AchievementIcon.STREAK, AchievementMetric.DAYS_PLAYED, 3,
            "Three Day Streak", "ثلاثة أيام متتالية",
            "Practise on 3 different days", "تدرّب في 3 أيام مختلفة"),
        a("routine_pro", AchievementIcon.ROUTINE, AchievementMetric.ROUTINE_ANSWERS, 12,
            "Daily Routine Pro", "بطل الروتين اليومي",
            "Match 12 real life moments", "طابق 12 لحظة من يومك"),
        a("speedy", AchievementIcon.SPEED, AchievementMetric.FAST_ANSWERS, 15,
            "Quick Thinker", "تفكير سريع",
            "Answer 15 questions in under 8 seconds", "أجب عن 15 سؤالاً في أقل من 8 ثوانٍ"),
        a("player_level_5", AchievementIcon.LEVEL, AchievementMetric.PLAYER_LEVEL, 5,
            "Level 5 Hero", "بطل المستوى الخامس",
            "Reach player level 5", "اصل إلى المستوى الخامس"),
        a("player_level_10", AchievementIcon.GRADUATE, AchievementMetric.PLAYER_LEVEL, 10,
            "Time Traveller", "مسافر عبر الزمن",
            "Reach player level 10", "اصل إلى المستوى العاشر")
    )

    fun byId(id: String): Achievement = items.first { it.id == id }
}
