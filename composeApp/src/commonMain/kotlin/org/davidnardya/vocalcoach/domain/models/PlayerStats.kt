package org.davidnardya.vocalcoach.domain.models

import kotlinx.datetime.LocalDate
import kotlin.math.floor

data class PlayerStats(
    val totalScore: Double, // Changed to Double to handle fractional points (0.5)
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
        fun levelFromTotalScore(totalScore: Double): Int =
            ((floor(totalScore) / 10.0) + 1.0).toInt()

        fun withDayRollover(stats: PlayerStats, today: LocalDate): PlayerStats {
            val todayStr = today.toString()
            if (stats.levelsGainedForDate == null) {
                // Anchor legacy/missing day key so midnight rollover can reset counts.
                return stats.copy(levelsGainedForDate = todayStr)
            }
            if (stats.levelsGainedForDate == todayStr) return stats
            return stats.copy(levelsGainedToday = 0, levelsGainedForDate = todayStr)
        }

        fun fromLegacyIntScore(score: Int): PlayerStats = PlayerStats(
            totalScore = score.toDouble(),
            streakCount = 0,
            lastLevelUpDate = null,
            lastStreakIncrementDate = null,
            levelsGainedToday = 0,
            levelsGainedForDate = null,
        )
    }
}
