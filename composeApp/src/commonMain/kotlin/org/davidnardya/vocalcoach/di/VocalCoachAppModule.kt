package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.domain.AudioClassificationUseCase
import org.davidnardya.vocalcoach.domain.AudioClassifier
import org.davidnardya.vocalcoach.domain.AudioRecorder

expect fun createAudioClassifier(): AudioClassifier

expect fun createAudioRecorder(): AudioRecorder

fun createAudioClassificationUseCase(): AudioClassificationUseCase {
    return AudioClassificationUseCase(
        classifier = createAudioClassifier(),
        recorder = createAudioRecorder(),
    )
}
