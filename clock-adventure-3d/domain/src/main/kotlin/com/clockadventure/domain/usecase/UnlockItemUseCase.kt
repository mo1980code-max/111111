package com.clockadventure.domain.usecase

import com.clockadventure.domain.model.Unlockable
import com.clockadventure.domain.model.UserProgress
import com.clockadventure.domain.repository.ProgressRepository
import javax.inject.Inject

enum class UnlockOutcome { UNLOCKED, ALREADY_OWNED, NOT_ENOUGH_COINS, LOCKED_BY_STARS }

/**
 * Buys an unlockable from the Rewards screen.
 *
 * An item can be bought with coins as soon as the child collected enough stars for it, so the
 * shop never shows a price the child cannot reach.
 */
class UnlockItemUseCase @Inject constructor(
    private val repository: ProgressRepository
) {

    suspend operator fun invoke(item: Unlockable, progress: UserProgress): UnlockOutcome {
        if (item.isDefault) return UnlockOutcome.ALREADY_OWNED
        if (repository.isUnlocked(item.id)) return UnlockOutcome.ALREADY_OWNED
        if (progress.stars < item.requiredStars) return UnlockOutcome.LOCKED_BY_STARS
        if (progress.coins < item.coinCost) return UnlockOutcome.NOT_ENOUGH_COINS
        return if (repository.purchase(item)) UnlockOutcome.UNLOCKED else UnlockOutcome.NOT_ENOUGH_COINS
    }
}
