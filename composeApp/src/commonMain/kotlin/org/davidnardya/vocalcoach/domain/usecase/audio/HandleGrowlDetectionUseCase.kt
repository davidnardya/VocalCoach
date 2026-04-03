package org.davidnardya.vocalcoach.domain.usecase.audio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.models.VocalValidationResult
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.game.ChallengeProvider
import org.davidnardya.vocalcoach.domain.usecase.game.LevelUpCelebration
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase

/**
 * UI feedback from a growl validation pass (partial hint, score event, STT calibration text).
 */
data class GrowlUiFeedback(
    val partialMessage: String? = null,
    val scoredPoints: Boolean = false,
    val aiHeard: String? = null,
    val isAnalyzing: Boolean = false,
)

class HandleGrowlDetectionUseCase(
    private val repository: ScoreRepository,
    private val streakManagementUseCase: StreakManagementUseCase,
    private val challengeProvider: ChallengeProvider,
    private val vocalValidator: VocalValidator,
) {
    private var lastDetectionTime: Long = 0
    private val cooldownMs = 2000L // Increased cooldown for Whisper processing
    private val statsUpdateMutex = Mutex()

    suspend operator fun invoke(snapshot: ClassificationSnapshot, stats: PlayerStats): GrowlUiFeedback {
        val challenge = challengeProvider.getChallenge(stats.level, stats.currentChallengeIndex)
        
        val validation = vocalValidator.validate(
            snapshot = snapshot,
            challenge = challenge,
            audioBuffer = snapshot.audioBuffer,
            stats = stats,
        )

        when (validation) {
            is VocalValidationResult.PartialSuccess -> {
                return GrowlUiFeedback(
                    partialMessage = validation.message,
                    aiHeard = validation.heardTranscription,
                )
            }
            is VocalValidationResult.Failure -> {
                return GrowlUiFeedback(aiHeard = validation.heardTranscription)
            }
            is VocalValidationResult.Success -> {
                val points = validation.points.toLong()
                val currentTime = Clock.System.now().toEpochMilliseconds()
                if (currentTime - lastDetectionTime <= cooldownMs) {
                    return GrowlUiFeedback(
                        scoredPoints = false,
                        aiHeard = validation.heardTranscription,
                    )
                }
                lastDetectionTime = currentTime

                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val todayStr = today.toString()

                var celebration: LevelUpCelebration? = null

                statsUpdateMutex.withLock {
                    repository.updateStats { raw ->
                        val rolled = PlayerStats.withDayRollover(raw, today)

                        var total = rolled.totalScore + points
                        val oldLevel = rolled.level
                        val newLevel = PlayerStats.levelFromTotalScore(total)

                        val nextChallengeIndex = rolled.currentChallengeIndex + 1

                        if (newLevel <= oldLevel) {
                            return@updateStats rolled.copy(
                                totalScore = total,
                                currentChallengeIndex = nextChallengeIndex,
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
                            currentChallengeIndex = nextChallengeIndex,
                        )
                    }
                }

                celebration?.let { c ->
                    streakManagementUseCase.emitLevelUpCelebration(c)
                    c.milestoneBonus?.let { bonus ->
                        streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
                    }
                }

                return GrowlUiFeedback(
                    scoredPoints = true,
                    aiHeard = validation.heardTranscription,
                )
            }
        }
    }
}
