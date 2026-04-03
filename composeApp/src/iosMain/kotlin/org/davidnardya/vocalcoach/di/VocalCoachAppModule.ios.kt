package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.data.IosNotificationScheduler
import org.davidnardya.vocalcoach.data.IosPcmAudioRecorder
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import org.davidnardya.vocalcoach.domain.usecase.audio.TranscriptionService

/**
 * Note: createAudioClassifier() is implemented in target-specific source sets 
 * (iosArm64Main and iosSimulatorArm64Main) to handle the LiteRT binary differences.
 */

actual fun createAudioRecorder(): AudioRecorder = IosPcmAudioRecorder()

actual fun createNotificationScheduler(): NotificationScheduler = IosNotificationScheduler()

actual fun createTranscriptionService(): TranscriptionService = TranscriptionService()
