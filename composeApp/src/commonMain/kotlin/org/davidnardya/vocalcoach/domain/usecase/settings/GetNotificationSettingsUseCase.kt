package org.davidnardya.vocalcoach.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import org.davidnardya.vocalcoach.domain.models.NotificationSettings
import org.davidnardya.vocalcoach.domain.repository.NotificationSettingsRepository

class GetNotificationSettingsUseCase(
    private val repository: NotificationSettingsRepository
) {
    operator fun invoke(): Flow<NotificationSettings> = repository.getSettings()
}
