package org.davidnardya.vocalcoach.domain.usecase.game

import kotlin.math.floor

/**
 * Hot-streak band for the tiered scoring table (session consecutive successes for the hit being scored).
 */
enum class HotStreakTier {
    /** Streak length 1–5 (inclusive). */
    Low,

    /** Streak length 6–15 (inclusive). */
    Mid,

    /** Streak length 16+. */
    High,
    ;

    companion object {
        fun fromConsecutiveSuccessCount(count: Int): HotStreakTier = when {
            count >= 16 -> High
            count >= 6 -> Mid
            else -> Low
        }
    }
}

/**
 * Whether the player is on their progression frontier or replaying an earlier stage.
 * Uses 1-based stage indices (e.g. Stage 1 → 1, Stage 5 → 5).
 */
enum class StageScoringMode {
    /** Practicing: [activeStage] < [maxUnlockedStage]. */
    PracticingPrevious,

    /** Progressing: [activeStage] == [maxUnlockedStage] (or edge: active ≥ max). */
    ProgressingCurrent,
    ;

    companion object {
        fun fromStages(activeStage: Int, maxUnlockedStage: Int): StageScoringMode =
            if (activeStage < maxUnlockedStage) PracticingPrevious else ProgressingCurrent
    }
}

/**
 * Tiered scoring: stage role (practicing vs progressing) × hot-streak band.
 *
 * @param hotStreakCount Consecutive successes **including** this hit (1 = first success in the session).
 */
class CalculatePointsUseCase {

    fun pointsForHit(
        activeStage: Int,
        maxUnlockedStage: Int,
        hotStreakCount: Int,
    ): Double {
        val mode = StageScoringMode.fromStages(activeStage, maxUnlockedStage)
        val tier = HotStreakTier.fromConsecutiveSuccessCount(hotStreakCount)
        return when (mode) {
            StageScoringMode.PracticingPrevious -> when (tier) {
                HotStreakTier.Low -> 0.5
                HotStreakTier.Mid -> 1.0
                HotStreakTier.High -> 2.0
            }
            StageScoringMode.ProgressingCurrent -> when (tier) {
                HotStreakTier.Low -> 1.0
                HotStreakTier.Mid -> 2.0
                HotStreakTier.High -> 3.0
            }
        }
    }

    /** Points for the next successful hit if the current session streak before the hit is [sessionStreakBeforeHit]. */
    fun previewPointsForNextHit(
        activeStage: Int,
        maxUnlockedStage: Int,
        sessionStreakBeforeHit: Int,
    ): Double = pointsForHit(
        activeStage = activeStage,
        maxUnlockedStage = maxUnlockedStage,
        hotStreakCount = sessionStreakBeforeHit + 1,
    )

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
