package com.clockadventure.domain.model

/**
 * A little multiplication a grown-up has to solve.
 *
 * It guards two things a child must not reach alone: the parent dashboard (statistics, daily limit
 * and the reset button) and the "fifteen more minutes" request after the daily limit was reached.
 */
data class GateQuestion(
    val a: Int,
    val b: Int,
    val options: List<Int>
) {
    val answer: Int get() = a * b

    /** Same in every language: it is a sum, not a sentence. */
    fun text(): String = "$a × $b = ?"
}
