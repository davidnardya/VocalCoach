package org.davidnardya.vocalcoach.di

import org.davidnardya.vocalcoach.data.IosTfLiteAudioClassifierAdapter
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier

actual fun createAudioClassifier(): AudioClassifier = IosTfLiteAudioClassifierAdapter()
