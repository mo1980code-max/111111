package com.clockadventure.data.repository

import com.clockadventure.data.db.AchievementDao
import com.clockadventure.data.db.AchievementEntity
import com.clockadventure.data.db.DailyStatDao
import com.clockadventure.data.db.DailyStatEntity
import com.clockadventure.data.db.GameScoreDao
import com.clockadventure.data.db.GameScoreEntity
import com.clockadventure.data.db.LessonProgressDao
import com.clockadventure.data.db.LessonProgressEntity
import com.clockadventure.data.db.UnlockDao
import com.clockadventure.data.db.UnlockEntity
import com.clockadventure.data.db.UserProfileDao
import com.clockadventure.data.db.UserProfileEntity
import com.clockadventure.data.db.toDomain
import com.clockadventure.data.util.DateKeys
import com.clockadventure.domain.catalog.GameId
import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.engine.AchievementEvaluator
import com.clockadventure.domain.engine.RewardEngine
import com.clockadventure.domain.model.Achievement
import com.clockadventure.domain.model.AchievementState
import com.clockadventure.domain.model.DailyRewardResult
import com.clockadventure.domain.model.DailyStat
import com.clockadventure.domain.model.Difficulty
import com.clockadventure.domain.model.GameScore
import com.clockadventure.domain.model.LessonCompletionResult
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.UnlockState
import com.clockadventure.domain.model.Unlockable
import com.clockadventure.domain.model.RewardCatalog
import com.clockadventure.domain.model.UserProgress
import com.clockadventure.domain.repository.AnswerEvent
import com.clockadventure.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room backed progress storage.
 *
 * Every write goes through a [Mutex] so a fast tapper cannot lose coins or stars, and every read
 * is a cold flow: the screens simply re-render when the child earns something.
 *
 * Answers are recorded one by one ([recordAnswer]); finishing a lesson ([completeLesson]) then
 * only adds the summary - stars, coins, xp and the newly reached achievements.
 */
@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val profileDao: UserProfileDao,
    private val lessonDao: LessonProgressDao,
    private val achievementDao: AchievementDao,
    private val unlockDao: UnlockDao,
    private val dailyDao: DailyStatDao,
    private val gameDao: GameScoreDao
) : ProgressRepository {

    private val mutex = Mutex()

    // ------------------------------------------------------------------ reading

    override fun observeProgress(): Flow<UserProgress> =
        profileDao.observe().map { it?.toDomain() ?: UserProgress() }

    override fun observeLessons(): Flow<List<LessonProgress>> =
        lessonDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAchievements(): Flow<List<AchievementState>> =
        combine(profileDao.observe(), achievementDao.observeAll()) { profile, unlocked ->
            AchievementEvaluator.evaluate(
                progress = profile?.toDomain() ?: UserProgress(),
                unlockedIds = unlocked.map { it.id }.toSet()
            )
        }

    override fun observeUnlockStates(): Flow<List<UnlockState>> =
        unlockDao.observeAll().map { unlocked ->
            val owned = unlocked.map { it.itemId }.toSet()
            RewardCatalog.items.map { item ->
                UnlockState(unlockable = item, unlocked = item.isDefault || owned.contains(item.id))
            }
        }

    override fun observeGameScores(): Flow<List<GameScore>> =
        gameDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeDailyStats(days: Int): Flow<List<DailyStat>> =
        dailyDao.observeRecent(days).map { rows ->
            val byKey = rows.associate { it.dateKey to it.toDomain() }
            DateKeys.recentKeys(days).map { key -> byKey[key] ?: DailyStat(dateKey = key) }
        }

    override fun observeTodayStat(): Flow<DailyStat> {
        val today = DateKeys.today()
        return dailyDao.observe(today).map { it?.toDomain() ?: DailyStat(dateKey = today) }
    }

    // ------------------------------------------------------------------ writing

    override suspend fun recordAnswer(event: AnswerEvent): Unit = mutex.withLock {
        val now = System.currentTimeMillis()
        val today = DateKeys.today()
        val profile = ensureProfile()

        val streak = when {
            DateKeys.isToday(profile.lastActiveDate) -> profile.streakDays
            DateKeys.isYesterday(profile.lastActiveDate) -> profile.streakDays + 1
            else -> 1
        }
        val isFirstVisitToday = !DateKeys.isToday(profile.lastActiveDate)
        val seconds = event.playSeconds.coerceAtLeast(0)

        profileDao.upsert(
            profile.copy(
                totalCorrect = profile.totalCorrect + if (event.correct) 1 else 0,
                totalWrong = profile.totalWrong + if (event.correct) 0 else 1,
                routineCorrect = profile.routineCorrect + if (event.correct && event.routine) 1 else 0,
                fastCorrect = profile.fastCorrect + if (event.correct && event.fast) 1 else 0,
                totalPlaySeconds = profile.totalPlaySeconds + seconds,
                streakDays = streak,
                daysPlayed = profile.daysPlayed + if (isFirstVisitToday) 1 else 0,
                lastActiveDate = today,
                updatedAt = now
            )
        )

        if (event.levelId > 0) {
            val lesson = lessonDao.get(event.levelId) ?: LessonProgressEntity(levelId = event.levelId)
            lessonDao.upsert(
                lesson.copy(
                    correct = lesson.correct + if (event.correct) 1 else 0,
                    wrong = lesson.wrong + if (event.correct) 0 else 1,
                    playSeconds = lesson.playSeconds + seconds,
                    updatedAt = now
                )
            )
        }

        val stat = ensureDayRow(today)
        dailyDao.upsert(
            stat.copy(
                correct = stat.correct + if (event.correct) 1 else 0,
                wrong = stat.wrong + if (event.correct) 0 else 1,
                secondsLearned = stat.secondsLearned + seconds
            )
        )

        syncAchievements()
    }

    override suspend fun recordSession(seconds: Int): Unit = mutex.withLock {
        ensureProfile()
        dailyDao.addSession(DateKeys.today())
        profileDao.addPlaySeconds(seconds)
        val key = DateKeys.today()
        val stat = ensureDayRow(key)
        dailyDao.upsert(stat.copy(secondsLearned = stat.secondsLearned + seconds.coerceAtLeast(0)))
    }

    override suspend fun completeLesson(
        levelId: Int,
        stars: Int,
        correct: Int,
        wrong: Int,
        hintsUsed: Int,
        flawless: Boolean,
        durationMs: Long,
        difficulty: Difficulty
    ): LessonCompletionResult = completeLessonInternal(levelId, stars, correct, wrong, hintsUsed, flawless, durationMs, difficulty)

    private suspend fun completeLessonInternal(
        levelId: Int,
        stars: Int,
        correct: Int,
        wrong: Int,
        hintsUsed: Int,
        flawless: Boolean,
        durationMs: Long,
        difficulty: Difficulty
    ): LessonCompletionResult = mutex.withLock {
        val now = System.currentTimeMillis()
        val profile = ensureProfile()
        val lesson = lessonDao.get(levelId) ?: LessonProgressEntity(levelId = levelId)
        val isNewBest = stars > lesson.bestStars
        val seconds = (durationMs / 1000L).toInt().coerceAtLeast(0)

        lessonDao.upsert(
            lesson.copy(
                bestStars = maxOf(lesson.bestStars, stars),
                completions = lesson.completions + 1,
                playSeconds = lesson.playSeconds + seconds,
                updatedAt = now
            )
        )

        val lessons = lessonDao.getAll()
        val totalStars = lessons.sumOf { it.bestStars }
        val completedLessons = lessons.count { it.bestStars > 0 }
        val nextLevelId = LevelCatalog.levels
            .firstOrNull { spec -> (lessons.firstOrNull { it.levelId == spec.id }?.bestStars ?: 0) == 0 }
            ?.id ?: levelId

        val coins = RewardEngine.coinsFor(stars, correct, isNewBest, flawless)
        val xp = RewardEngine.xpFor(correct, stars, difficulty)
        val levelBefore = RewardEngine.playerLevelFor(profile.xp)
        val levelAfter = RewardEngine.playerLevelFor(profile.xp + xp)

        profileDao.upsert(
            profile.copy(
                xp = profile.xp + xp,
                coins = profile.coins + coins,
                stars = totalStars,
                lessonsCompleted = completedLessons,
                flawlessLessons = profile.flawlessLessons + if (flawless) 1 else 0,
                totalPlaySeconds = profile.totalPlaySeconds + seconds,
                currentLevel = nextLevelId,
                updatedAt = now
            )
        )

        ensureDayRow(DateKeys.today())
        dailyDao.addSession(DateKeys.today())

        val achievements = syncAchievements()
        val unlocks = newlyAvailableUnlocks(previousStars = profile.stars, newStars = totalStars)

        LessonCompletionResult(
            stars = stars,
            coinsEarned = coins,
            xpEarned = xp,
            newAchievements = achievements,
            newUnlocks = unlocks,
            leveledUp = levelAfter > levelBefore,
            isNewBest = isNewBest,
            unlockedNextLevelId = if (nextLevelId != levelId) nextLevelId else null,
            currentLevel = levelAfter
        )
    }

    override suspend fun recordGameScore(gameId: GameId, score: Int): Unit = mutex.withLock {
        val current = gameDao.get(gameId.name) ?: GameScoreEntity(gameId = gameId.name)
        gameDao.upsert(
            current.copy(
                bestScore = maxOf(current.bestScore, score),
                plays = current.plays + 1
            )
        )
        ensureDayRow(DateKeys.today())
        dailyDao.addSession(DateKeys.today())
        syncAchievements()
    }

    override suspend fun addCoins(amount: Int) = mutex.withLock {
        ensureProfile()
        profileDao.addCoins(amount)
    }

    override suspend fun addXp(amount: Int) = mutex.withLock {
        ensureProfile()
        profileDao.addXp(amount)
    }

    override suspend fun purchase(item: Unlockable): Boolean = mutex.withLock {
        val profile = ensureProfile()
        if (profile.coins < item.coinCost) {
            false
        } else {
            val updated = profileDao.spendCoins(item.coinCost)
            if (updated > 0) {
                unlockDao.insert(UnlockEntity(itemId = item.id, unlockedAt = System.currentTimeMillis()))
                true
            } else {
                false
            }
        }
    }

    override suspend fun isUnlocked(itemId: String): Boolean = unlockDao.count(itemId) > 0

    override suspend fun claimDailyReward(): DailyRewardResult? = mutex.withLock {
        val profile = ensureProfile()
        val today = DateKeys.today()
        if (DateKeys.isToday(profile.lastDailyRewardDate)) {
            null
        } else {
            val streak = if (DateKeys.isYesterday(profile.lastActiveDate)) profile.streakDays + 1 else 1
            val coins = RewardEngine.dailyRewardCoins(streak)
            val xp = RewardEngine.dailyRewardXp(streak)
            profileDao.upsert(
                profile.copy(
                    coins = profile.coins + coins,
                    xp = profile.xp + xp,
                    streakDays = streak,
                    lastDailyRewardDate = today,
                    lastActiveDate = today,
                    updatedAt = System.currentTimeMillis()
                )
            )
            DailyRewardResult(coins = coins, xp = xp, streakDays = streak)
        }
    }

    override suspend fun addPlaySeconds(seconds: Int) = mutex.withLock {
        ensureProfile()
        profileDao.addPlaySeconds(seconds)
        val stat = ensureDayRow(DateKeys.today())
        dailyDao.upsert(stat.copy(secondsLearned = stat.secondsLearned + seconds.coerceAtLeast(0)))
    }

    override suspend fun setCurrentLevel(levelId: Int) = mutex.withLock {
        ensureProfile()
        profileDao.setCurrentLevel(levelId)
    }

    override suspend fun markIntroSeen() = mutex.withLock {
        ensureProfile()
        profileDao.markIntroSeen()
    }

    override suspend fun resetProgress() = mutex.withLock {
        profileDao.clear()
        lessonDao.clear()
        achievementDao.clear()
        unlockDao.clear()
        dailyDao.clear()
        gameDao.clear()
    }

    override suspend fun unlockedItems(): List<Unlockable> {
        val owned = unlockDao.getAll().map { it.itemId }.toSet()
        return RewardCatalog.items.filter { it.isDefault || owned.contains(it.id) }
    }

    // ------------------------------------------------------------------ helpers

    private suspend fun ensureProfile(): UserProfileEntity {
        val existing = profileDao.get()
        if (existing != null) return existing
        val fresh = UserProfileEntity(updatedAt = System.currentTimeMillis())
        profileDao.upsert(fresh)
        return fresh
    }

    private suspend fun ensureDayRow(dateKey: String): DailyStatEntity {
        val existing = dailyDao.get(dateKey)
        if (existing != null) return existing
        val fresh = DailyStatEntity(dateKey = dateKey)
        dailyDao.upsert(fresh)
        return fresh
    }

    /** Unlocks every achievement whose goal is reached and returns the new ones. */
    private suspend fun syncAchievements(): List<Achievement> {
        val profile = profileDao.get() ?: return emptyList()
        val unlockedIds = achievementDao.getAll().map { it.id }.toSet()
        val states = AchievementEvaluator.evaluate(profile.toDomain(), unlockedIds)
        val fresh = states.filter { it.unlocked && !unlockedIds.contains(it.achievement.id) }
        fresh.forEach { state ->
            achievementDao.insert(AchievementEntity(id = state.achievement.id, unlockedAt = System.currentTimeMillis()))
        }
        return fresh.map { it.achievement }
    }

    /** Items that became affordable because the child crossed their star threshold. */
    private fun newlyAvailableUnlocks(previousStars: Int, newStars: Int): List<Unlockable> =
        RewardCatalog.items.filter { item ->
            !item.isDefault && item.requiredStars > previousStars && item.requiredStars <= newStars
        }
}
