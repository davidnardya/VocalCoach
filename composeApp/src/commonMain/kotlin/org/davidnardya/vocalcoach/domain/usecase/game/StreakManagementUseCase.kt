package org.davidnardya.vocalcoach.domain.usecase.game

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

/**
 * Startup inactivity penalties + level-up bookkeeping.
 * Day streak increments and +25 milestones are handled by [UpdateStreakUseCase].
 */
class StreakManagementUseCase(
    private val repository: ScoreRepository,
    private val handleLevelUpUseCase: HandleLevelUpUseCase,
    /**
     * When true, allows streak 24→25 even if [PlayerStats.lastStreakIncrementDate] is already today
     * (bypasses once-per-day gating for milestone/UI testing). Set to `false` for production.
     */
    private val isDebugStreakTest: Boolean = false,
) {
    private val mutex = Mutex()
    private var startupDone = false

    private val _milestoneBonusEvents = MutableSharedFlow<MilestoneBonus>(extraBufferCapacity = 1)
    val milestoneBonusEvents: SharedFlow<MilestoneBonus> = _milestoneBonusEvents.asSharedFlow()

    private val _levelUpCelebrations = MutableSharedFlow<LevelUpCelebration>(extraBufferCapacity = 1)
    val levelUpCelebrations: SharedFlow<LevelUpCelebration> = _levelUpCelebrations.asSharedFlow()

    suspend fun emitMilestoneBonusForPresentation(bonus: MilestoneBonus) {
        _milestoneBonusEvents.emit(bonus)
    }

    suspend fun emitLevelUpCelebration(celebration: LevelUpCelebration) {
        _levelUpCelebrations.emit(celebration)
    }

    /**
     * Called when a level-up occurred ([newLevel] > [oldLevel]).
     * Does not modify day streak; streak is updated via [UpdateStreakUseCase].
     */
    fun processLevelUpBranch(
        rolled: PlayerStats,
        oldLevel: Int,
        newLevel: Int,
        todayStr: String,
        totalScoreAfterGrowl: Double,
    ): LevelUpProcessResult {
        require(newLevel > oldLevel)

        val lastLevelUp = todayStr
        val deltaLevels = newLevel - oldLevel
        val levelsState = handleLevelUpUseCase.nextLevelsGainedState(
            rolled = rolled,
            todayStr = todayStr,
            deltaLevels = deltaLevels,
        )
        val levelsToday = levelsState.levelsGainedToday
        val levelsDay = levelsState.levelsGainedForDate

        return LevelUpProcessResult(
            totalScore = totalScoreAfterGrowl,
            currentStreak = rolled.currentStreak,
            lastLevelUpDate = lastLevelUp,
            lastStreakIncrementDate = rolled.lastStreakIncrementDate,
            levelsGainedToday = levelsToday,
            levelsGainedForDate = levelsDay,
            milestoneBonus = null,
        )
    }

    suspend fun runStartupCheck(): PenaltyResult = mutex.withLock {
        if (startupDone) return PenaltyResult.None
        startupDone = true

        var result: PenaltyResult = PenaltyResult.None
        repository.updateStats { stats ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val rolled = PlayerStats.withDayRollover(stats, today)
            val lastStr = rolled.lastLevelUpDate
            if (lastStr == null) {
                result = PenaltyResult.None
                return@updateStats rolled
            }
            val last = LocalDate.parse(lastStr)
            val daysBetween = today.toEpochDays() - last.toEpochDays()
            if (daysBetween <= 1) {
                result = PenaltyResult.None
                return@updateStats rolled
            }

            val penaltyDays = daysBetween - 1
            val penalty = 5.0 * penaltyDays
            val newScore = (rolled.totalScore - penalty).coerceAtLeast(0.0)
            val streakReset = newScore < 5.0
            val newStreak = if (streakReset) 0 else rolled.currentStreak

            result = PenaltyResult.Applied(
                pointsDeducted = penalty.toLong(),
                streakReset = streakReset,
                message = buildString {
                    append("Missed days penalty: -${penalty.toInt()} points")
                    if (streakReset) append(". Streak reset!")
                },
            )

            rolled.copy(
                totalScore = newScore,
                currentStreak = newStreak,
                lastCompletionTimestamp = if (streakReset) null else rolled.lastCompletionTimestamp,
            )
        }
        result
    }
}

data class LevelUpProcessResult(
    val totalScore: Double,
    val currentStreak: Int,
    val lastLevelUpDate: String?,
    val lastStreakIncrementDate: String?,
    val levelsGainedToday: Int,
    val levelsGainedForDate: String?,
    val milestoneBonus: MilestoneBonus?,
)
