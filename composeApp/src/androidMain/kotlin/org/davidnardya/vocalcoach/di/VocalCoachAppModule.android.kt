package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.data.AndroidNotificationScheduler
import org.davidnardya.vocalcoach.data.AndroidPcmAudioRecorder
import org.davidnardya.vocalcoach.data.TfLiteTaskAudioClassifierAdapter
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import org.davidnardya.vocalcoach.domain.usecase.audio.TranscriptionService
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

actual fun createAudioClassifier(): AudioClassifier {
    return TfLiteTaskAudioClassifierAdapter(AndroidPlatformContext.application)
}

actual fun createAudioRecorder(): AudioRecorder = AndroidPcmAudioRecorder()

actual fun createNotificationScheduler(): NotificationScheduler {
    return AndroidNotificationScheduler(AndroidPlatformContext.application)
}

actual fun createTranscriptionService(): TranscriptionService {
    return TranscriptionService(AndroidPlatformContext.application)
}
