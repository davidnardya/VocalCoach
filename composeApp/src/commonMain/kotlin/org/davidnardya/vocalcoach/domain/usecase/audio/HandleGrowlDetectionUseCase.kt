package org.davidnardya.vocalcoach.domain.usecase.audio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.LevelUpCelebration
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.MilestoneBonus
import org.davidnardya.vocalcoach.domain.usecase.game.UpdateStreakUseCase

class HandleGrowlDetectionUseCase(
    private val repository: ScoreRepository,
    private val calculatePointsUseCase: CalculatePointsUseCase,
    private val streakManagementUseCase: StreakManagementUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
) {
    private var lastDetectionTime: Long = 0
    private val cooldownMs = 1500L
    private val statsUpdateMutex = Mutex()

    suspend operator fun invoke(snapshot: ClassificationSnapshot) {
        // This use case seems to be used in some contexts, but the ViewModel also has maybeScoreAttempt.
        // Let's ensure consistency if this is used.
        if (!snapshot.topLabel.contains("Growl", ignoreCase = true) || snapshot.topScore <= 0.5f) {
            return
        }
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (currentTime - lastDetectionTime <= cooldownMs) {
            return
        }
        lastDetectionTime = currentTime

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayStr = today.toString()

        var celebration: LevelUpCelebration? = null

        statsUpdateMutex.withLock {
            var streakMilestone: MilestoneBonus? = null
            repository.updateStats { raw ->
                val rolled = PlayerStats.withDayRollover(raw, today)
                val points = 1.0

                var total = rolled.totalScore + points
                val oldLevel = rolled.level
                val newLevel = PlayerStats.levelFromTotalScore(total)

                var next = if (newLevel <= oldLevel) {
                    rolled.copy(totalScore = total)
                } else {
                    val r = streakManagementUseCase.processLevelUpBranch(
                        rolled = rolled,
                        oldLevel = oldLevel,
                        newLevel = newLevel,
                        todayStr = todayStr,
                        totalScoreAfterGrowl = total,
                    )
                    celebration = LevelUpCelebration(newLevel = newLevel, milestoneBonus = r.milestoneBonus)
                    rolled.copy(
                        totalScore = r.totalScore,
                        currentStreak = r.currentStreak,
                        lastLevelUpDate = r.lastLevelUpDate,
                        lastStreakIncrementDate = r.lastStreakIncrementDate,
                        levelsGainedToday = r.levelsGainedToday,
                        levelsGainedForDate = r.levelsGainedForDate,
                    )
                }

                val streakResult = updateStreakUseCase.applySessionCompletion(next)
                streakMilestone = streakResult.milestoneBonus
                streakResult.stats
            }
            streakMilestone?.let { bonus ->
                streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
            }
        }

        celebration?.let { c ->
            streakManagementUseCase.emitLevelUpCelebration(c)
            c.milestoneBonus?.let { bonus ->
                streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
            }
        }
    }
}
