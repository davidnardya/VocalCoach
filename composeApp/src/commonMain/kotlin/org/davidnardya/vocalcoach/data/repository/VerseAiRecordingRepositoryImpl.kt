package org.davidnardya.vocalcoach.data.repository

import org.davidnardya.vocalcoach.data.gemini.AiVocalValidator
import org.davidnardya.vocalcoach.di.readRecordedAudioBytes
import org.davidnardya.vocalcoach.domain.models.AiVerseValidationResult
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.VerseAiRecordingRepository

class VerseAiRecordingRepositoryImpl(
    private val validator: AiVocalValidator,
    private val recorder: AudioRecorder,
    private val recordedMimeType: String,
) : VerseAiRecordingRepository {

    override val isRecording = recorder.isRecording

    override fun startRecording() {
        recorder.startRecording()
    }

    override suspend fun stopRecordingAndValidate(currentTarget: String): Result<AiVerseValidationResult> {
        val path = recorder.stopRecording()
            ?: return Result.failure(IllegalStateException("No recording file."))
        val bytes = readRecordedAudioBytes(path)
        if (bytes.isEmpty()) {
            return Result.failure(IllegalStateException("Empty audio file."))
        }
        return validator.validateVerse(bytes, recordedMimeType, currentTarget)
    }
}
