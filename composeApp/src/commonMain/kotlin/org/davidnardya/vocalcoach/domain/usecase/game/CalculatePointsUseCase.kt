package org.davidnardya.vocalcoach.domain.usecase.game

import kotlin.math.floor

/**
 * Enhanced scoring logic:
 * 1. Hot Streak (consecutive successes in current session):
 *    - Hits 1-5: 1 point
 *    - Hits 6-15: 2 points
 *    - Hits 16+: 3 points
 * 2. Previous Stage Penalty:
 *    - If playing a previous stage, points are halved (0.5x).
 *    - Hot streak results in previous stages: 0.5, 1.0, 1.5 points.
 * 3. Daily Bonus (Legacy support for HandleGrowlDetectionUseCase):
 *    - levelsGainedToday multiplier.
 */
class CalculatePointsUseCase {

    /** Combo-based points for a single growl. */
    fun pointsForGrowl(
        comboBeforeHit: Int,
        isPreviousStage: Boolean
    ): Double {
        val hitNumber = comboBeforeHit + 1
        val basePoints = when {
            hitNumber >= 16 -> 3.0
            hitNumber >= 6 -> 2.0
            else -> 1.0
        }

        return if (isPreviousStage) {
            basePoints * 0.5
        } else {
            basePoints
        }
    }

    /** Multiplier display for the NEXT hit. */
    fun getCurrentMultiplier(combo: Int, isPreviousStage: Boolean): Double {
        val hitNumber = combo + 1
        val base = when {
            hitNumber >= 16 -> 3.0
            hitNumber >= 6 -> 2.0
            else -> 1.0
        }
        return if (isPreviousStage) base * 0.5 else base
    }

    /** Legacy / Daily levels multiplier. */
    fun computeMultiplier(levelsGainedToday: Int): Double {
        return when {
            levelsGainedToday >= 10 -> 3.0
            levelsGainedToday >= 2 -> 2.0
            else -> 1.0
        }
    }

    /** Legacy / Daily points (fixed 1.0 base). */
    fun pointsForGrowl(levelsGainedTodayForMultiplier: Int): Double {
        return 1.0 * computeMultiplier(levelsGainedTodayForMultiplier)
    }

    /**
     * Truncates fractional points when leaving a session.
     */
    fun truncatePoints(totalScore: Double): Long {
        return floor(totalScore).toLong()
    }
}
