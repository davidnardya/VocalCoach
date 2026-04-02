package org.davidnardya.vocalcoach.domain.repository

import kotlinx.coroutines.flow.Flow
import org.davidnardya.vocalcoach.domain.models.NotificationSettings

interface NotificationSettingsRepository {
    fun getSettings(): Flow<NotificationSettings>
    suspend fun updateSettings(settings: NotificationSettings)
}
