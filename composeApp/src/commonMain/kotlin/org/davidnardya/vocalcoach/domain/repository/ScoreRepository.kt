package org.davidnardya.vocalcoach.domain.repository

import kotlinx.coroutines.flow.Flow
import org.davidnardya.vocalcoach.domain.models.PlayerStats

/**
 * Persistent player progress. [PlayerStats] includes [PlayerStats.currentStreak] and
 * [PlayerStats.lastCompletionTimestamp] for calendar-day streak rules (see domain UpdateStreakUseCase).
 */
interface ScoreRepository {
    fun getPlayerStats(): Flow<PlayerStats>

    suspend fun updateStats(transform: (PlayerStats) -> PlayerStats)

    suspend fun resetScore()
}
