package org.davidnardya.vocalcoach.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import org.davidnardya.vocalcoach.di.createAudioClassificationUseCase

@Composable
fun rememberAudioClassificationViewModel(): AudioClassificationViewModel {
    return viewModel {
        AudioClassificationViewModel(createAudioClassificationUseCase())
    }
}
