package org.davidnardya.vocalcoach.domain.usecase.game

import kotlin.math.roundToLong

/**
 * Hot-state scoring: base 1 point × multiplier from levels already gained today (after midnight rollover).
 * ×2 when [levelsGainedToday] >= 2, ×3 when >= 10.
 */
class CalculatePointsUseCase {

    fun computeMultiplier(levelsGainedToday: Int): Double = when {
        levelsGainedToday >= 10 -> 3.0
        levelsGainedToday >= 2 -> 2.0
        else -> 1.0
    }

    fun pointsForGrowl(levelsGainedTodayForMultiplier: Int): Long {
        val m = computeMultiplier(levelsGainedTodayForMultiplier)
        return maxOf(1L, (1.0 * m).roundToLong())
    }
}
