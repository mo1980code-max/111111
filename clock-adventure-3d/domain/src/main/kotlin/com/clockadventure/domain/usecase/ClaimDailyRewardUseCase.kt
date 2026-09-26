package com.clockadventure.domain.usecase

import com.clockadventure.domain.model.DailyRewardResult
import com.clockadventure.domain.repository.ProgressRepository
import javax.inject.Inject

/** Claims the once a day gift. Returns null when it was already claimed today. */
class ClaimDailyRewardUseCase @Inject constructor(
    private val repository: ProgressRepository
) {

    suspend operator fun invoke(): DailyRewardResult? = repository.claimDailyReward()
}
