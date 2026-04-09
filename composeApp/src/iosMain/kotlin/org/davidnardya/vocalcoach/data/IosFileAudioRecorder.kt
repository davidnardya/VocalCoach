package org.davidnardya.vocalcoach.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder

/**
 * Placeholder until iOS file capture is implemented.
 */
class IosFileAudioRecorder : AudioRecorder {

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    override fun startRecording() {
        _isRecording.value = true
    }

    override fun stopRecording(): String? {
        _isRecording.value = false
        return null
    }
}
