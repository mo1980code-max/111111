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
            .filter { it.answers >= 3 }

        val strong = topics.filter { it.accuracy >= STRONG_THRESHOLD }
            .sortedByDescending { it.accuracy }
            .take(3)
        val weak = topics.filter { it.accuracy < WEAK_THRESHOLD }
            .sortedBy { it.accuracy }
            .take(3)

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

    companion object {
        const val STRONG_THRESHOLD = 0.8f
        const val WEAK_THRESHOLD = 0.7f
    }
}
