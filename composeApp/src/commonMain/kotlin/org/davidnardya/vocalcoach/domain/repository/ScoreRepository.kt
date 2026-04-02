package org.davidnardya.vocalcoach.domain.repository

import kotlinx.coroutines.flow.Flow
import org.davidnardya.vocalcoach.domain.models.PlayerStats

interface ScoreRepository {
    fun getPlayerStats(): Flow<PlayerStats>
    suspend fun incrementScore()
    suspend fun resetScore()
}
