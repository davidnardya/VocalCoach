package org.davidnardya.vocalcoach.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import org.davidnardya.vocalcoach.data.local.producePath
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

class ScoreRepositoryImpl : ScoreRepository {
    /** Temporary default for testing the 25× streak milestone; revert to `0` for release. */
    private companion object {
        const val DefaultStreakCount = 24
    }

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath("growl_scores.preferences_pb").toPath() },
    )

    private val LEGACY_SCORE_INT = intPreferencesKey("total_score")
    private val TOTAL_SCORE_LONG = longPreferencesKey("total_score_long")
    private val STREAK_KEY = intPreferencesKey("streak_count")
    private val LAST_LEVEL_UP_KEY = stringPreferencesKey("last_level_up_date")
    private val LAST_STREAK_INC_KEY = stringPreferencesKey("last_streak_increment_date")
    private val LEVELS_TODAY_KEY = intPreferencesKey("levels_gained_today")
    private val LEVELS_DAY_KEY = stringPreferencesKey("levels_gained_for_date")

    override fun getPlayerStats(): Flow<PlayerStats> = dataStore.data.map { it.toPlayerStats() }

    override suspend fun updateStats(transform: (PlayerStats) -> PlayerStats) {
        dataStore.edit { preferences ->
            val current = preferences.toPlayerStats()
            val next = transform(current)
            preferences[TOTAL_SCORE_LONG] = next.totalScore
            preferences.remove(LEGACY_SCORE_INT)
            preferences[STREAK_KEY] = next.streakCount
            next.lastLevelUpDate?.let { preferences[LAST_LEVEL_UP_KEY] = it } ?: preferences.remove(LAST_LEVEL_UP_KEY)
            next.lastStreakIncrementDate?.let { preferences[LAST_STREAK_INC_KEY] = it }
                ?: preferences.remove(LAST_STREAK_INC_KEY)
            preferences[LEVELS_TODAY_KEY] = next.levelsGainedToday
            next.levelsGainedForDate?.let { preferences[LEVELS_DAY_KEY] = it } ?: preferences.remove(LEVELS_DAY_KEY)
        }
    }

    override suspend fun resetScore() {
        dataStore.edit { preferences ->
            preferences[TOTAL_SCORE_LONG] = 0L
            preferences.remove(LEGACY_SCORE_INT)
            preferences[STREAK_KEY] = DefaultStreakCount
            preferences.remove(LAST_LEVEL_UP_KEY)
            preferences.remove(LAST_STREAK_INC_KEY)
            preferences[LEVELS_TODAY_KEY] = 0
            preferences.remove(LEVELS_DAY_KEY)
        }
    }

    private fun Preferences.toPlayerStats(): PlayerStats {
        val longScore = this[TOTAL_SCORE_LONG]
        val legacyInt = this[LEGACY_SCORE_INT]
        val total = longScore ?: legacyInt?.toLong() ?: 0L

        val hasNewModel = this[STREAK_KEY] != null ||
            this[LAST_LEVEL_UP_KEY] != null ||
            this[LAST_STREAK_INC_KEY] != null ||
            this[LEVELS_TODAY_KEY] != null ||
            longScore != null

        if (!hasNewModel && legacyInt != null) {
            return PlayerStats.fromLegacyIntScore(legacyInt)
        }

        return PlayerStats(
            totalScore = total,
            streakCount = this[STREAK_KEY] ?: DefaultStreakCount,
            lastLevelUpDate = this[LAST_LEVEL_UP_KEY],
            lastStreakIncrementDate = this[LAST_STREAK_INC_KEY],
            levelsGainedToday = this[LEVELS_TODAY_KEY] ?: 0,
            levelsGainedForDate = this[LEVELS_DAY_KEY],
        )
    }
}
