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
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.GetNotificationSettingsUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.ResetScoreUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.SaveSettingsUseCase

expect fun createAudioClassifier(): AudioClassifier
expect fun createAudioRecorder(): AudioRecorder
expect fun createNotificationScheduler(): NotificationScheduler

private val scoreRepository: ScoreRepository by lazy { ScoreRepositoryImpl() }
private val notificationSettingsRepository: NotificationSettingsRepository by lazy { NotificationSettingsRepositoryImpl() }
private val calculatePointsUseCase: CalculatePointsUseCase by lazy { CalculatePointsUseCase() }

fun createAudioClassificationUseCase(): AudioClassificationUseCase {
    return AudioClassificationUseCase(
        classifier = createAudioClassifier(),
        recorder = createAudioRecorder(),
    )
}

fun createLevelTrackerUseCase(): LevelTrackerUseCase = LevelTrackerUseCase(scoreRepository)
fun createScoreRepository(): ScoreRepository = scoreRepository
fun createCalculatePointsUseCase(): CalculatePointsUseCase = calculatePointsUseCase
fun createStreakManagementUseCase(): StreakManagementUseCase =
    StreakManagementUseCase(scoreRepository, isDebugStreakTest = true)

/** Must receive the same [StreakManagementUseCase] instance as [AudioClassificationViewModel] so milestone/level-up flows reach the UI. */
fun createHandleGrowlDetectionUseCase(streakManagement: StreakManagementUseCase): HandleGrowlDetectionUseCase =
    HandleGrowlDetectionUseCase(
        scoreRepository,
        calculatePointsUseCase,
        streakManagement,
    )

fun createGetNotificationSettingsUseCase() = GetNotificationSettingsUseCase(notificationSettingsRepository)
fun createSaveSettingsUseCase() = SaveSettingsUseCase(notificationSettingsRepository, createNotificationScheduler())
fun createNotificationSettingsRepository() = notificationSettingsRepository

fun createResetScoreUseCase() = ResetScoreUseCase(scoreRepository)
