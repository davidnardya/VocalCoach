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
 * Startup inactivity penalties + streak milestone bonuses (+20 every 25 streak days).
 * [processLevelUpBranch] contains the once-per-day streak increment and milestone check.
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
    val milestoneBonusEvents: SharedFlow<StreakMilestoneEvent> = _milestoneBonusEvents.asSharedFlow()

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
     * Applies daily streak increment (at most once per calendar day) and +20 when [streakCount] hits 25, 50, …
     * Bonus is included in [LevelUpProcessResult.totalScore] (persist immediately via [ScoreRepository.updateStats]).
     */
    fun processLevelUpBranch(
        rolled: PlayerStats,
        oldLevel: Int,
        newLevel: Int,
        todayStr: String,
        totalScoreAfterGrowl: Long,
    ): LevelUpProcessResult {
        require(newLevel > oldLevel)

        var lastLevelUp = todayStr
        val deltaLevels = newLevel - oldLevel
        val levelsState = handleLevelUpUseCase.nextLevelsGainedState(
            rolled = rolled,
            todayStr = todayStr,
            deltaLevels = deltaLevels,
        )
        var levelsToday = levelsState.levelsGainedToday
        val levelsDay = levelsState.levelsGainedForDate

        var streak = rolled.streakCount
        var lastStreakInc = rolled.lastStreakIncrementDate
        var total = totalScoreAfterGrowl
        var milestone: MilestoneBonus? = null

        val streakIncToday = lastStreakInc == todayStr
        val allowStreakIncrement = !streakIncToday || (isDebugStreakTest && streak == 24)
        if (allowStreakIncrement) {
            streak += 1
            lastStreakInc = todayStr
            if (streak > 0 && streak % 25 == 0) {
                total += 20L
                milestone = MilestoneBonus(streakCount = streak, bonusPoints = 20L)
            }
        }

        return LevelUpProcessResult(
            totalScore = total,
            streakCount = streak,
            lastLevelUpDate = lastLevelUp,
            lastStreakIncrementDate = lastStreakInc,
            levelsGainedToday = levelsToday,
            levelsGainedForDate = levelsDay,
            milestoneBonus = milestone,
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
            val penalty = 5L * penaltyDays
            val newScore = (rolled.totalScore - penalty).coerceAtLeast(0L)
            val streakReset = newScore < 5L
            val newStreak = if (streakReset) 0 else rolled.streakCount

            result = PenaltyResult.Applied(
                pointsDeducted = penalty,
                streakReset = streakReset,
                message = buildString {
                    append("Missed days penalty: -$penalty points")
                    if (streakReset) append(". Streak reset!")
                },
            )

            rolled.copy(totalScore = newScore, streakCount = newStreak)
        }
        result
    }
}

data class LevelUpProcessResult(
    val totalScore: Long,
    val streakCount: Int,
    val lastLevelUpDate: String?,
    val lastStreakIncrementDate: String?,
    val levelsGainedToday: Int,
    val levelsGainedForDate: String?,
    val milestoneBonus: MilestoneBonus?,
)
