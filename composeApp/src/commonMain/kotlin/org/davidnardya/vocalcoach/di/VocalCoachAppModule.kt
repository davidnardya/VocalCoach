package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.data.repository.NotificationSettingsRepositoryImpl
import org.davidnardya.vocalcoach.data.repository.ScoreRepositoryImpl
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import org.davidnardya.vocalcoach.domain.repository.NotificationSettingsRepository
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.audio.AudioClassificationUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.HandleGrowlDetectionUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.LevelTrackerUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.GetNotificationSettingsUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.ResetScoreUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.SaveSettingsUseCase

expect fun createAudioClassifier(): AudioClassifier
expect fun createAudioRecorder(): AudioRecorder
expect fun createNotificationScheduler(): NotificationScheduler

private val scoreRepository: ScoreRepository by lazy { ScoreRepositoryImpl() }
private val notificationSettingsRepository: NotificationSettingsRepository by lazy { NotificationSettingsRepositoryImpl() }

fun createAudioClassificationUseCase(): AudioClassificationUseCase {
    return AudioClassificationUseCase(
        classifier = createAudioClassifier(),
        recorder = createAudioRecorder(),
    )
}

fun createLevelTrackerUseCase(): LevelTrackerUseCase = LevelTrackerUseCase(scoreRepository)
fun createScoreRepository(): ScoreRepository = scoreRepository
fun createHandleGrowlDetectionUseCase(): HandleGrowlDetectionUseCase = HandleGrowlDetectionUseCase(scoreRepository)

fun createGetNotificationSettingsUseCase() = GetNotificationSettingsUseCase(notificationSettingsRepository)
fun createSaveSettingsUseCase() = SaveSettingsUseCase(notificationSettingsRepository, createNotificationScheduler())
fun createNotificationSettingsRepository() = notificationSettingsRepository//todo - why no use?

fun createResetScoreUseCase() = ResetScoreUseCase(scoreRepository)
