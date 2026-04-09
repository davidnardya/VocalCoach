package org.davidnardya.vocalcoach.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import okio.Path.Companion.toPath
import org.davidnardya.vocalcoach.data.local.producePath
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

class ScoreRepositoryImpl : ScoreRepository {
    private companion object {
        const val DefaultStreakCount = 0
    }

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath("growl_scores.preferences_pb").toPath() },
    )

    private val LEGACY_SCORE_INT = intPreferencesKey("total_score")
    private val LEGACY_SCORE_LONG = longPreferencesKey("total_score_long")
    private val TOTAL_SCORE_DOUBLE = doublePreferencesKey("total_score_double")
    private val STREAK_KEY = intPreferencesKey("streak_count")
    private val LAST_COMPLETION_TIMESTAMP_MS = longPreferencesKey("last_completion_timestamp_ms")
    private val LAST_LEVEL_UP_KEY = stringPreferencesKey("last_level_up_date")
    private val LAST_STREAK_INC_KEY = stringPreferencesKey("last_streak_increment_date")
    private val LEVELS_TODAY_KEY = intPreferencesKey("levels_gained_today")
    private val LEVELS_DAY_KEY = stringPreferencesKey("levels_gained_for_date")

    override fun getPlayerStats(): Flow<PlayerStats> = dataStore.data.map { it.toPlayerStats() }

    override suspend fun updateStats(transform: (PlayerStats) -> PlayerStats) {
        dataStore.edit { preferences ->
            val current = preferences.toPlayerStats()
            val next = transform(current)
            preferences[TOTAL_SCORE_DOUBLE] = next.totalScore
            preferences.remove(LEGACY_SCORE_INT)
            preferences.remove(LEGACY_SCORE_LONG)
            preferences[STREAK_KEY] = next.currentStreak
            next.lastCompletionTimestamp?.let { preferences[LAST_COMPLETION_TIMESTAMP_MS] = it }
                ?: preferences.remove(LAST_COMPLETION_TIMESTAMP_MS)
            next.lastLevelUpDate?.let { preferences[LAST_LEVEL_UP_KEY] = it } ?: preferences.remove(LAST_LEVEL_UP_KEY)
            next.lastStreakIncrementDate?.let { preferences[LAST_STREAK_INC_KEY] = it }
                ?: preferences.remove(LAST_STREAK_INC_KEY)
            preferences[LEVELS_TODAY_KEY] = next.levelsGainedToday
            next.levelsGainedForDate?.let { preferences[LEVELS_DAY_KEY] = it } ?: preferences.remove(LEVELS_DAY_KEY)
        }
    }

    override suspend fun resetScore() {
        dataStore.edit { preferences ->
            preferences[TOTAL_SCORE_DOUBLE] = 0.0
            preferences.remove(LEGACY_SCORE_INT)
            preferences.remove(LEGACY_SCORE_LONG)
            preferences[STREAK_KEY] = DefaultStreakCount
            preferences.remove(LAST_COMPLETION_TIMESTAMP_MS)
            preferences.remove(LAST_LEVEL_UP_KEY)
            preferences.remove(LAST_STREAK_INC_KEY)
            preferences[LEVELS_TODAY_KEY] = 0
            preferences.remove(LEVELS_DAY_KEY)
        }
    }

    private fun Preferences.toPlayerStats(): PlayerStats {
        val doubleScore = this[TOTAL_SCORE_DOUBLE]
        val longScore = this[LEGACY_SCORE_LONG]
        val legacyInt = this[LEGACY_SCORE_INT]
        
        val total = doubleScore ?: longScore?.toDouble() ?: legacyInt?.toDouble() ?: 0.0

        val streak = this[STREAK_KEY] ?: DefaultStreakCount
        val storedLastCompletion = this[LAST_COMPLETION_TIMESTAMP_MS]
        val zone = TimeZone.currentSystemDefault()
        // Legacy: had streak but no timestamp — anchor to start of today so streak doesn't jump on first new session.
        val effectiveLastCompletion = storedLastCompletion ?: if (streak > 0) {
            Clock.System.todayIn(zone).atStartOfDayIn(zone).toEpochMilliseconds()
        } else {
            null
        }

        return PlayerStats(
            totalScore = total,
            currentStreak = streak,
            lastCompletionTimestamp = effectiveLastCompletion,
            lastLevelUpDate = this[LAST_LEVEL_UP_KEY],
            lastStreakIncrementDate = this[LAST_STREAK_INC_KEY],
            levelsGainedToday = this[LEVELS_TODAY_KEY] ?: 0,
            levelsGainedForDate = this[LEVELS_DAY_KEY],
        )
    }
}
