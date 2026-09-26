package com.clockadventure.domain.usecase

import com.clockadventure.domain.catalog.LevelCatalog
import com.clockadventure.domain.engine.RewardEngine
import com.clockadventure.domain.model.LessonProgress
import com.clockadventure.domain.model.LessonState
import com.clockadventure.domain.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Merges the level catalogue with the stored progress into ready to render level cards. */
class ObserveLevelsUseCase @Inject constructor() {

    operator fun invoke(
        lessons: Flow<List<LessonProgress>>,
        progress: Flow<UserProgress>
    ): Flow<List<LessonState>> = combine(lessons, progress) { lessonList, userProgress ->
        val byLevel = lessonList.associateBy { it.levelId }
        LevelCatalog.levels.map { spec ->
            val previousStars = byLevel[spec.id - 1]?.bestStars ?: 0
            val unlocked = RewardEngine.isLevelUnlocked(spec.id, previousStars)
            LessonState(
                spec = spec,
                progress = byLevel[spec.id] ?: LessonProgress(levelId = spec.id),
                unlocked = unlocked,
                isCurrent = userProgress.currentLevelId == spec.id
            )
        }
    }
}
