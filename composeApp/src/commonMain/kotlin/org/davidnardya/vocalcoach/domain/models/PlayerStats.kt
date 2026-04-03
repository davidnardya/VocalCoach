package org.davidnardya.vocalcoach.domain.models

import kotlinx.datetime.LocalDate

data class PlayerStats(
    val totalScore: Long,
    val streakCount: Int,
    /** ISO calendar date (yyyy-MM-dd) of last level-up (used for inactivity penalty). */
    val lastLevelUpDate: String?,
    /** ISO calendar date of last time the daily streak counter was incremented (max once per day). */
    val lastStreakIncrementDate: String?,
    val levelsGainedToday: Int,
    /** Which day [levelsGainedToday] refers to; reset at local midnight when this != today. */
    val levelsGainedForDate: String?,
) {
    val level: Int
        get() = levelFromTotalScore(totalScore)

    companion object {
        fun levelFromTotalScore(totalScore: Long): Int =
            ((totalScore / 10L) + 1L).toInt()

        fun withDayRollover(stats: PlayerStats, today: LocalDate): PlayerStats {
            val todayStr = today.toString()
            val marker = stats.levelsGainedForDate ?: return stats
            if (marker == todayStr) return stats
            return stats.copy(levelsGainedToday = 0, levelsGainedForDate = todayStr)
        }

        fun fromLegacyIntScore(score: Int): PlayerStats = PlayerStats(
            totalScore = score.toLong(),
            // Match ScoreRepositoryImpl default streak for milestone testing (revert to 0 for release).
            streakCount = 24,
            lastLevelUpDate = null,
            lastStreakIncrementDate = null,
            levelsGainedToday = 0,
            levelsGainedForDate = null,
        )
    }
}
