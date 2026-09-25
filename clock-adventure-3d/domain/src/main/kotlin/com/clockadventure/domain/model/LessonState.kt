package com.clockadventure.domain.model

import com.clockadventure.domain.catalog.LessonSpec

/** A learning level together with everything the UI needs to draw its card. */
data class LessonState(
    val spec: LessonSpec,
    val progress: LessonProgress,
    val unlocked: Boolean,
    val isCurrent: Boolean
) {
    val stars: Int get() = progress.bestStars
    val levelId: Int get() = spec.id
    val accuracyPercent: Int get() = (progress.accuracy * 100f).toInt()
}
