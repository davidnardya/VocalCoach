package org.davidnardya.vocalcoach.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import org.davidnardya.vocalcoach.data.local.producePath
import org.davidnardya.vocalcoach.domain.models.NotificationSettings
import org.davidnardya.vocalcoach.domain.repository.NotificationSettingsRepository

class NotificationSettingsRepositoryImpl : NotificationSettingsRepository {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath("notification_settings.preferences_pb").toPath() }
    )

    private val ENABLED_KEY = booleanPreferencesKey("enabled")
    private val HOUR_KEY = intPreferencesKey("hour")
    private val MINUTE_KEY = intPreferencesKey("minute")
    private val INTERVAL_HOURS_KEY = longPreferencesKey("interval_hours")

    override fun getSettings(): Flow<NotificationSettings> = dataStore.data.map { preferences ->
        // Safely read intervalHours as it might have been stored as an Int in previous versions
        val intervalHoursValue = preferences.asMap().entries.find { it.key.name == "interval_hours" }?.value
        val intervalHours = (intervalHoursValue as? Number)?.toLong() ?: 24L

        NotificationSettings(
            enabled = preferences[ENABLED_KEY] ?: true,
            hour = preferences[HOUR_KEY] ?: 17,
            minute = preferences[MINUTE_KEY] ?: 0,
            intervalHours = intervalHours
        )
    }

    override suspend fun updateSettings(settings: NotificationSettings) {
        dataStore.edit { preferences ->
            preferences[ENABLED_KEY] = settings.enabled
            preferences[HOUR_KEY] = settings.hour
            preferences[MINUTE_KEY] = settings.minute
            preferences[INTERVAL_HOURS_KEY] = settings.intervalHours
        }
    }
}
