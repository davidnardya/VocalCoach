package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.data.IosNotificationScheduler
import org.davidnardya.vocalcoach.data.IosPcmAudioRecorder
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler

actual fun createAudioRecorder(): AudioRecorder = IosPcmAudioRecorder()

actual fun createNotificationScheduler(): NotificationScheduler = IosNotificationScheduler()
