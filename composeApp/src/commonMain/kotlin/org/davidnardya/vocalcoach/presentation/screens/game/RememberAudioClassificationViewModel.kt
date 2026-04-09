package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.di.createAiVocalValidator
import org.davidnardya.vocalcoach.di.createAudioClassificationUseCase
import org.davidnardya.vocalcoach.di.createCalculatePointsUseCase
import org.davidnardya.vocalcoach.di.createFileAudioRecorder
import org.davidnardya.vocalcoach.di.createGetNextTargetUseCase
import org.davidnardya.vocalcoach.di.createUpdateStreakUseCase
import org.davidnardya.vocalcoach.di.createLevelTrackerUseCase
import org.davidnardya.vocalcoach.di.createScoreRepository
import org.davidnardya.vocalcoach.di.createStreakManagementUseCase

@Composable
fun rememberAudioClassificationViewModel(stage: TrainingStage): AudioClassificationViewModel {
    return viewModel {
        val streakManagement = createStreakManagementUseCase()
        val updateStreakUseCase = createUpdateStreakUseCase()
        AudioClassificationViewModel(
            stage = stage,
            audioUseCase = createAudioClassificationUseCase(stage, versePcmCapture = null),
            repository = createScoreRepository(),
            levelTrackerUseCase = createLevelTrackerUseCase(),
            calculatePointsUseCase = createCalculatePointsUseCase(),
            streakManagementUseCase = streakManagement,
            updateStreakUseCase = updateStreakUseCase,
            getNextTargetUseCase = createGetNextTargetUseCase(),
            fileAudioRecorder = createFileAudioRecorder(),
            aiVocalValidator = if (stage.ordinal >= TrainingStage.Stage3.ordinal) {
                createAiVocalValidator()
            } else {
                null
            },
        )
    }
}
