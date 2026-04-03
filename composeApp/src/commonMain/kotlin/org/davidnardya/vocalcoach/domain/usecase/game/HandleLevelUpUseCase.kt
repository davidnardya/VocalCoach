package org.davidnardya.vocalcoach.domain.usecase.game

import org.davidnardya.vocalcoach.domain.models.PlayerStats

/**
 * Daily level-gain counter for score multipliers.
 *
 * Persist [levelsGainedToday] / [levelsGainedForDate] via [org.davidnardya.vocalcoach.domain.repository.ScoreRepository.updateStats]
 * in the **same** transaction as score changes (see [org.davidnardya.vocalcoach.domain.usecase.audio.HandleGrowlDetectionUseCase]).
 */
class HandleLevelUpUseCase {

    /**
     * @param rolled Stats after [PlayerStats.withDayRollover]; [rolled.levelsGainedToday] is the persisted value before this level-up.
     * @param deltaLevels How many levels were gained in this update (usually [newLevel] - [oldLevel]).
     */
    fun nextLevelsGainedState(
        rolled: PlayerStats,
        todayStr: String,
        deltaLevels: Int,
    ): LevelsGainedState {
        require(deltaLevels >= 1) { "deltaLevels must be at least 1" }
        val next = rolled.levelsGainedToday + deltaLevels
        val day = rolled.levelsGainedForDate ?: todayStr
        return LevelsGainedState(levelsGainedToday = next, levelsGainedForDate = day)
    }
}

data class LevelsGainedState(
    val levelsGainedToday: Int,
    val levelsGainedForDate: String,
)
