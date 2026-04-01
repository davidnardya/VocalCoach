package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.data.IosPcmAudioRecorder
import org.davidnardya.vocalcoach.data.IosTfLiteAudioClassifierAdapter
import org.davidnardya.vocalcoach.domain.AudioClassifier
import org.davidnardya.vocalcoach.domain.AudioRecorder

actual fun createAudioClassifier(): AudioClassifier = IosTfLiteAudioClassifierAdapter()

actual fun createAudioRecorder(): AudioRecorder = IosPcmAudioRecorder()
