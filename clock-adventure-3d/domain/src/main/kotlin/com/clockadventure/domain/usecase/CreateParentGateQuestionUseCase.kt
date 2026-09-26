package com.clockadventure.domain.usecase

import com.clockadventure.domain.model.GateQuestion
import javax.inject.Inject
import kotlin.random.Random

/**
 * Builds the gate question.
 *
 * The rules are deliberately strict, because a broken gate would either lock a parent out of their
 * own dashboard or let a child through:
 * * both factors stay small enough to be solved in the head (4..9 × 3..8);
 * * the correct answer is always one of the four options;
 * * the four options are all different and all positive;
 * * the options are shuffled, so the answer is never in the same place.
 */
class CreateParentGateQuestionUseCase @Inject constructor() {

    operator fun invoke(random: Random = Random.Default): GateQuestion {
        val a = random.nextInt(4, 10)
        val b = random.nextInt(3, 9)
        val answer = a * b
        val options = LinkedHashSet<Int>(4)
        options.add(answer)

        var guard = 0
        while (options.size < OPTIONS && guard++ < 100) {
            val candidate = (answer + random.nextInt(-6, 7)).coerceIn(1, MAX_OPTION)
            if (candidate != answer) options.add(candidate)
        }
        // Deterministic fallback: the random walk above can - in theory - keep hitting the same
        // values, and a gate with duplicate or missing options would be unsolvable.
        var step = 1
        while (options.size < OPTIONS) {
            val candidate = (answer + step * 3).coerceIn(1, MAX_OPTION)
            if (candidate != answer) options.add(candidate)
            step++
        }
        return GateQuestion(a = a, b = b, options = options.toList().shuffled(random))
    }

    companion object {
        const val OPTIONS = 4
        const val MAX_OPTION = 99
    }
}
