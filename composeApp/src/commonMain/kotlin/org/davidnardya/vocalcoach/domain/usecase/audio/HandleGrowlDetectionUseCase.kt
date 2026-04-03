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

class HandleGrowlDetectionUseCase(
    private val repository: ScoreRepository,
    private val calculatePointsUseCase: CalculatePointsUseCase,
    private val streakManagementUseCase: StreakManagementUseCase,
) {
    private var lastDetectionTime: Long = 0
    private val cooldownMs = 1500L
    private val statsUpdateMutex = Mutex()

    suspend operator fun invoke(snapshot: ClassificationSnapshot) {
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
            repository.updateStats { raw ->
                val rolled = PlayerStats.withDayRollover(raw, today)
                val levelsForMultiplier = rolled.levelsGainedToday
                val points = calculatePointsUseCase.pointsForGrowl(levelsForMultiplier)

                var total = rolled.totalScore + points
                val oldLevel = rolled.level
                val newLevel = PlayerStats.levelFromTotalScore(total)

                if (newLevel <= oldLevel) {
                    return@updateStats rolled.copy(
                        totalScore = total,
                    )
                }

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
                    streakCount = r.streakCount,
                    lastLevelUpDate = r.lastLevelUpDate,
                    lastStreakIncrementDate = r.lastStreakIncrementDate,
                    levelsGainedToday = r.levelsGainedToday,
                    levelsGainedForDate = r.levelsGainedForDate,
                )
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
