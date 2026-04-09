package org.davidnardya.vocalcoach.domain.usecase.game

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

/**
 * Calendar-day streak rules (local timezone):
 * - Same day as last completion: streak unchanged.
 * - Next calendar day: increment streak; optional +20 milestone when crossing 25, 50, …
 * - Any gap of more than one calendar day: reset streak to 1.
 *
 * [applySessionCompletion] is pure date logic — call from repository [updateStats] transforms only.
 */
class UpdateStreakUseCase(
    private val repository: ScoreRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

    /** For UI: observe streak without date math in ViewModel. */
    fun observeCurrentStreak(): Flow<Int> =
        repository.getPlayerStats().map { it.currentStreak }

    /**
     * Applies streak rules after a successful session. Does not modify non-streak fields except
     * [PlayerStats.totalScore] when a milestone bonus applies.
     */
    fun applySessionCompletion(stats: PlayerStats): StreakSessionResult {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(timeZone).date
        val lastTs = stats.lastCompletionTimestamp

        if (lastTs == null) {
            val updated = stats.copy(
                currentStreak = 1,
                lastCompletionTimestamp = now.toEpochMilliseconds(),
            )
            return StreakSessionResult(stats = updated, milestoneBonus = null)
        }

        val lastDate = Instant.fromEpochMilliseconds(lastTs).toLocalDateTime(timeZone).date

        if (lastDate == today) {
            return StreakSessionResult(stats = stats, milestoneBonus = null)
        }

        val dayDiff = today.toEpochDays() - lastDate.toEpochDays()
        return when {
            dayDiff == 1 -> {
                val previousStreak = stats.currentStreak
                val newStreak = previousStreak + 1
                var next = stats.copy(
                    currentStreak = newStreak,
                    lastCompletionTimestamp = now.toEpochMilliseconds(),
                )
                var milestone: MilestoneBonus? = null
                if (newStreak > 0 && newStreak % 25 == 0) {
                    next = next.copy(totalScore = next.totalScore + 20.0)
                    milestone = MilestoneBonus(streakCount = newStreak, bonusPoints = 20L)
                }
                StreakSessionResult(stats = next, milestoneBonus = milestone)
            }
            else -> {
                StreakSessionResult(
                    stats = stats.copy(
                        currentStreak = 1,
                        lastCompletionTimestamp = now.toEpochMilliseconds(),
                    ),
                    milestoneBonus = null,
                )
            }
        }
    }

    /**
     * Developer override: sets [currentStreak] and [lastCompletionTimestamp] to start of yesterday
     * (local calendar) so the streak stays valid for activity today.
     */
    suspend fun applyManualStreakOverride(currentStreak: Int) {
        val safe = currentStreak.coerceAtLeast(0)
        repository.updateStats { stats ->
            if (safe == 0) {
                return@updateStats stats.copy(
                    currentStreak = 0,
                    lastCompletionTimestamp = null,
                )
            }
            val todayCal: LocalDate = Clock.System.todayIn(timeZone)
            val yesterday = LocalDate.fromEpochDays(todayCal.toEpochDays() - 1)
            val instant = yesterday.atStartOfDayIn(timeZone)
            stats.copy(
                currentStreak = safe,
                lastCompletionTimestamp = instant.toEpochMilliseconds(),
            )
        }
    }
}

data class StreakSessionResult(
    val stats: PlayerStats,
    val milestoneBonus: MilestoneBonus?,
)
