package com.clockadventure.domain.model

/** How the child is doing on one learning level. */
data class TopicStat(
    val levelId: Int,
    val title: LocalizedText,
    val stars: Int,
    val answers: Int,
    val accuracy: Float
) {
    val accuracyPercent: Int get() = (accuracy * 100f).toInt()
}

/** Everything the parent dashboard shows. */
data class ParentReport(
    val totalPlaySeconds: Int,
    val todaySeconds: Int,
    val accuracy: Float,
    val correct: Int,
    val wrong: Int,
    val lessonsCompleted: Int,
    val stars: Int,
    val coins: Int,
    val xp: Int,
    val playerLevel: Int,
    val streakDays: Int,
    val strongTopics: List<TopicStat>,
    val weakTopics: List<TopicStat>,
    val dailyStats: List<DailyStat>,
    val averageSessionSeconds: Int
) {
    val accuracyPercent: Int get() = (accuracy * 100f).toInt()

    val totalPlayMinutes: Int get() = totalPlaySeconds / 60

    val todayMinutes: Int get() = todaySeconds / 60
}
