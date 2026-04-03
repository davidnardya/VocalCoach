package org.davidnardya.vocalcoach.domain.repository

import kotlinx.coroutines.flow.Flow
import org.davidnardya.vocalcoach.domain.models.PlayerStats

interface ScoreRepository {
    fun getPlayerStats(): Flow<PlayerStats>

    suspend fun updateStats(transform: (PlayerStats) -> PlayerStats)

    /** Bypasses merge/transform logic: overwrites persisted stats as-is (e.g. developer tools). */
    suspend fun writePlayerStats(stats: PlayerStats)

    suspend fun resetScore()
}
