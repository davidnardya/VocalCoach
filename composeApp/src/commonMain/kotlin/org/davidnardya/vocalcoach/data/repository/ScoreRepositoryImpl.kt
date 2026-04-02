package org.davidnardya.vocalcoach.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import org.davidnardya.vocalcoach.data.local.producePath
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

class ScoreRepositoryImpl : ScoreRepository {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath("growl_scores.preferences_pb").toPath() }
    )

    private val SCORE_KEY = intPreferencesKey("total_score")

    override fun getPlayerStats(): Flow<PlayerStats> = dataStore.data.map { preferences ->
        val score = preferences[SCORE_KEY] ?: 0
        PlayerStats.fromScore(score)
    }

    override suspend fun incrementScore() {
        dataStore.edit { preferences ->
            val current = preferences[SCORE_KEY] ?: 0
            preferences[SCORE_KEY] = current + 1
        }
    }

    override suspend fun resetScore() {
        dataStore.edit { preferences ->
            preferences[SCORE_KEY] = 0
        }
    }
}
