package org.davidnardya.vocalcoach.domain.usecase.game

/** One-time +20 score bonus when [streakCount] hits a multiple of 25. */
data class MilestoneBonus(
    val streakCount: Int,
    val bonusPoints: Long = 20L,
)

/** Emitted on [StreakManagementUseCase.milestoneBonusEvents] when a streak milestone is awarded. */
typealias StreakMilestoneEvent = MilestoneBonus

data class LevelUpCelebration(
    val newLevel: Int,
    val milestoneBonus: MilestoneBonus?,
)
