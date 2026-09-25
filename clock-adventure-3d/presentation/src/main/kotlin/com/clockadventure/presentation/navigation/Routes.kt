package com.clockadventure.presentation.navigation

/** Navigation destinations. Arguments are kept simple so the graph stays readable. */
object Routes {
    const val HOME = "home"
    const val LESSONS = "lessons"
    const val CHALLENGES = "challenges"
    const val GAMES = "games"
    const val PROGRESS = "progress"
    const val REWARDS = "rewards"
    const val SETTINGS = "settings"
    const val PARENT_GATE = "parent_gate"
    const val PARENT = "parent"

    const val ARG_LEVEL_ID = "levelId"
    const val ARG_GAME_ID = "gameId"
    const val ARG_CHALLENGE_ID = "challengeId"

    const val SESSION = "session?levelId={levelId}&gameId={gameId}&challengeId={challengeId}"

    fun session(levelId: Int = -1, gameId: String? = null, challengeId: String? = null): String {
        val query = mutableListOf<String>()
        if (levelId > 0) query += "$ARG_LEVEL_ID=$levelId"
        if (!gameId.isNullOrBlank()) query += "$ARG_GAME_ID=$gameId"
        if (!challengeId.isNullOrBlank()) query += "$ARG_CHALLENGE_ID=$challengeId"
        return if (query.isEmpty()) "session" else "session?" + query.joinToString("&")
    }
}
