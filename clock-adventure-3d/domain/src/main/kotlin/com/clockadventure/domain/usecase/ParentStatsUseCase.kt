package com.clockadventure.domain.usecase

import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.model.DailyStat
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.ParentReport
import com.clockadventure.domain.model.TopicStat
import com.clockadventure.domain.model.UserProgress
import javax.inject.Inject

/** Aggregates the raw progress into the report shown in the parent dashboard. */
class ParentStatsUseCase @Inject constructor() {

    operator fun invoke(
        progress: UserProgress,
        lessons: List<LessonProgress>,
        dailyStats: List<DailyStat>,
        today: DailyStat
    ): ParentReport {
        val (strong, weak) = topicStats(lessons)

        val sessions = dailyStats.sumOf { it.sessions }
        val seconds = dailyStats.sumOf { it.secondsLearned }

        return ParentReport(
            totalPlaySeconds = progress.totalPlaySeconds,
            todaySeconds = today.secondsLearned,
            accuracy = progress.accuracy,
            correct = progress.totalCorrect,
            wrong = progress.totalWrong,
            lessonsCompleted = progress.lessonsCompleted,
            stars = progress.stars,
            coins = progress.coins,
            xp = progress.xp,
            playerLevel = progress.playerLevel,
            streakDays = progress.streakDays,
            strongTopics = strong,
            weakTopics = weak,
            dailyStats = dailyStats,
            averageSessionSeconds = if (sessions == 0) 0 else seconds / sessions
        )
    }

    /**
     * The strongest and the weakest three levels the child has actually practised.
     *
     * Kept public because the parent dashboard shows both lists and turns every weak topic into a
     * one-tap shortcut back to that lesson.
     */
    fun topicStats(lessons: List<LessonProgress>): Pair<List<TopicStat>, List<TopicStat>> {
        val byLevel = lessons.associateBy { it.levelId }
        val topics = LevelCatalog.levels
            .map { spec ->
                val lesson = byLevel[spec.id] ?: LessonProgress(levelId = spec.id)
                TopicStat(
                    levelId = spec.id,
                    title = spec.title,
                    stars = lesson.bestStars,
                    answers = lesson.answers,
                    accuracy = lesson.accuracy
                )
            }
            .filter { it.answers >= MIN_ANSWERS }

        val strong = topics.filter { it.accuracy >= STRONG_THRESHOLD }
            .sortedByDescending { it.accuracy }
            .take(3)
        val weak = topics.filter { it.accuracy < WEAK_THRESHOLD }
            .sortedBy { it.accuracy }
            .take(3)
        return strong to weak
    }

    companion object {
        const val STRONG_THRESHOLD = 0.8f
        const val WEAK_THRESHOLD = 0.7f
        /** A level is judged only after the child answered at least this many questions. */
        const val MIN_ANSWERS = 3
    }
}
