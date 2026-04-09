package org.davidnardya.vocalcoach.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Captures a single take to a local file for upload (e.g. Gemini multimodal).
 */
interface AudioRecorder {
    val isRecording: StateFlow<Boolean>

    fun startRecording()

    /**
     * Stops capture and returns the absolute path to the audio file, or null if nothing was recorded.
     */
    fun stopRecording(): String?
}
