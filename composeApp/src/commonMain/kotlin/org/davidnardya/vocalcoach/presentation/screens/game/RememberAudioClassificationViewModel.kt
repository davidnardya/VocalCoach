package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.davidnardya.vocalcoach.di.createAudioClassificationUseCase
import org.davidnardya.vocalcoach.di.createCalculatePointsUseCase
import org.davidnardya.vocalcoach.di.createChallengeProvider
import org.davidnardya.vocalcoach.di.createHandleGrowlDetectionUseCase
import org.davidnardya.vocalcoach.di.createLevelTrackerUseCase
import org.davidnardya.vocalcoach.di.createScoreRepository
import org.davidnardya.vocalcoach.di.createStreakManagementUseCase

@Composable
fun rememberAudioClassificationViewModel(): AudioClassificationViewModel {
    return viewModel {
        val streakManagement = createStreakManagementUseCase()
        AudioClassificationViewModel(
            audioUseCase = createAudioClassificationUseCase(),
            repository = createScoreRepository(),
            levelTrackerUseCase = createLevelTrackerUseCase(),
            handleGrowlDetectionUseCase = createHandleGrowlDetectionUseCase(streakManagement),
            calculatePointsUseCase = createCalculatePointsUseCase(),
            streakManagementUseCase = streakManagement,
            challengeProvider = createChallengeProvider(),
        )
    }
}
