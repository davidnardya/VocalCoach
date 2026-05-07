package org.davidnardya.vocalcoach.domain.usecase.settings

import org.davidnardya.vocalcoach.domain.models.NotificationSettings
import org.davidnardya.vocalcoach.domain.repository.NotificationSettingsRepository

class SaveSettingsUseCase(
    private val repository: NotificationSettingsRepository
) {
    suspend operator fun invoke(settings: NotificationSettings) {
        repository.updateSettings(settings)
    }
}
