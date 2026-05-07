package org.davidnardya.vocalcoach.di

import io.ktor.client.HttpClient
import org.davidnardya.vocalcoach.data.audio.TeeAudioRecorder
import org.davidnardya.vocalcoach.data.audio.VersePcmCapture
import org.davidnardya.vocalcoach.data.gemini.AiVocalValidator
import org.davidnardya.vocalcoach.data.repository.VerseAiRecordingRepositoryImpl
import org.davidnardya.vocalcoach.data.repository.NotificationSettingsRepositoryImpl
import org.davidnardya.vocalcoach.data.repository.ScoreRepositoryImpl
import org.davidnardya.vocalcoach.data.repository.StageTargetDeckRepositoryImpl
import org.davidnardya.vocalcoach.data.repository.VocalContentRepositoryImpl
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.PcmFrameAudioRecorder
import org.davidnardya.vocalcoach.domain.repository.VerseAiRecordingRepository
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import org.davidnardya.vocalcoach.domain.repository.NotificationSettingsRepository
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.repository.StageTargetDeckRepository
import org.davidnardya.vocalcoach.domain.repository.VocalContentRepository
import org.davidnardya.vocalcoach.domain.usecase.audio.AudioClassificationUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.HandleGrowlDetectionUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.LevelTrackerUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.GetNextTargetUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.HandleLevelUpUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.UpdateStreakUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.GetNotificationSettingsUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.ResetScoreUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.SaveSettingsUseCase

expect fun createAudioClassifier(): AudioClassifier
expect fun createPcmFrameAudioRecorder(): PcmFrameAudioRecorder
expect fun createFileAudioRecorder(): AudioRecorder
expect fun readRecordedAudioBytes(path: String): ByteArray
expect fun createNotificationScheduler(): NotificationScheduler
expect fun createGeminiHttpClient(): HttpClient
expect fun readGeminiApiKey(): String

private val scoreRepository: ScoreRepository by lazy { ScoreRepositoryImpl() }
private val notificationSettingsRepository: NotificationSettingsRepository by lazy { NotificationSettingsRepositoryImpl() }
private val stageTargetDeckRepository: StageTargetDeckRepository by lazy { StageTargetDeckRepositoryImpl() }
private val vocalContentRepository: VocalContentRepository by lazy { VocalContentRepositoryImpl() }
private val calculatePointsUseCase: CalculatePointsUseCase by lazy { CalculatePointsUseCase() }
private val handleLevelUpUseCase: HandleLevelUpUseCase by lazy { HandleLevelUpUseCase() }
private val getNextTargetUseCase: GetNextTargetUseCase by lazy {
    GetNextTargetUseCase(vocalContentRepository, stageTargetDeckRepository)
}

fun createAudioClassificationUseCase(
    stage: TrainingStage,
    versePcmCapture: VersePcmCapture?,
): AudioClassificationUseCase {
    val baseRecorder = createPcmFrameAudioRecorder()
    val recorder =
        if (stage == TrainingStage.Stage5 && versePcmCapture != null) {
            TeeAudioRecorder(baseRecorder, versePcmCapture)
        } else {
            baseRecorder
        }
    return AudioClassificationUseCase(
        classifier = createAudioClassifier(),
        recorder = recorder,
    )
}

fun createAiVocalValidator(): AiVocalValidator =
    AiVocalValidator(createGeminiHttpClient(), readGeminiApiKey())

fun createVerseAiRecordingRepository(): VerseAiRecordingRepository =
    VerseAiRecordingRepositoryImpl(
        validator = createAiVocalValidator(),
        recorder = createFileAudioRecorder(),
        recordedMimeType = "audio/wav",
    )

fun createLevelTrackerUseCase(): LevelTrackerUseCase = LevelTrackerUseCase(scoreRepository)
fun createScoreRepository(): ScoreRepository = scoreRepository
fun createCalculatePointsUseCase(): CalculatePointsUseCase = calculatePointsUseCase
fun createHandleLevelUpUseCase(): HandleLevelUpUseCase = handleLevelUpUseCase
fun createStreakManagementUseCase(): StreakManagementUseCase =
    StreakManagementUseCase(scoreRepository, handleLevelUpUseCase, isDebugStreakTest = true)

fun createUpdateStreakUseCase(): UpdateStreakUseCase =
    UpdateStreakUseCase(scoreRepository)

/** Must receive the same [StreakManagementUseCase] instance as the game [org.davidnardya.vocalcoach.presentation.screens.game.AudioClassificationViewModel] so milestone/level-up flows reach the UI. */
fun createHandleGrowlDetectionUseCase(
    streakManagement: StreakManagementUseCase,
    updateStreakUseCase: UpdateStreakUseCase,
): HandleGrowlDetectionUseCase =
    HandleGrowlDetectionUseCase(
        scoreRepository,
        calculatePointsUseCase,
        streakManagement,
        updateStreakUseCase,
    )

fun createGetNotificationSettingsUseCase() = GetNotificationSettingsUseCase(notificationSettingsRepository)
fun createSaveSettingsUseCase() = SaveSettingsUseCase(notificationSettingsRepository)
fun createNotificationSettingsRepository() = notificationSettingsRepository

fun createResetScoreUseCase() = ResetScoreUseCase(scoreRepository)
fun createGetNextTargetUseCase(): GetNextTargetUseCase = getNextTargetUseCase
