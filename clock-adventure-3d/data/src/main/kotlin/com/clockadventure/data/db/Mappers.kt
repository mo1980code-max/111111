package com.clockadventure.data.db

import com.clockadventure.domain.model.DailyStat
import com.clockadventure.domain.model.GameScore
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.UserProgress

/** Entity <-> domain mapping. Kept here so the repository stays readable. */

fun UserProfileEntity.toDomain(): UserProgress = UserProgress(
    xp = xp,
    coins = coins,
    stars = stars,
    totalCorrect = totalCorrect,
    totalWrong = totalWrong,
    totalPlaySeconds = totalPlaySeconds,
    lessonsCompleted = lessonsCompleted,
    currentLevelId = currentLevel,
    flawlessLessons = flawlessLessons,
    daysPlayed = daysPlayed,
    routineCorrect = routineCorrect,
    fastCorrect = fastCorrect,
    streakDays = streakDays,
    lastActiveDateKey = lastActiveDate,
    lastDailyRewardDateKey = lastDailyRewardDate,
    parentVerified = parentVerified
)

fun LessonProgressEntity.toDomain(): LessonProgress = LessonProgress(
    levelId = levelId,
    bestStars = bestStars,
    completions = completions,
    correct = correct,
    wrong = wrong,
    playSeconds = playSeconds,
    updatedAt = updatedAt
)

fun DailyStatEntity.toDomain(): DailyStat = DailyStat(
    dateKey = dateKey,
    secondsLearned = secondsLearned,
    correct = correct,
    wrong = wrong,
    sessions = sessions
)

fun GameScoreEntity.toDomain(): GameScore = GameScore(
    gameId = gameId,
    bestScore = bestScore,
    plays = plays
)
