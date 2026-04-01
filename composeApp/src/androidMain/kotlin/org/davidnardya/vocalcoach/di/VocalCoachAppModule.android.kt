package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.data.AndroidPcmAudioRecorder
import org.davidnardya.vocalcoach.data.TfLiteTaskAudioClassifierAdapter
import org.davidnardya.vocalcoach.domain.AudioClassifier
import org.davidnardya.vocalcoach.domain.AudioRecorder
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

actual fun createAudioClassifier(): AudioClassifier {
    return TfLiteTaskAudioClassifierAdapter(AndroidPlatformContext.application)
}

actual fun createAudioRecorder(): AudioRecorder = AndroidPcmAudioRecorder()
