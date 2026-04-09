package org.davidnardya.vocalcoach.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.davidnardya.vocalcoach.domain.models.AiVerseValidationResult

/**
 * Records a take to disk and validates it with the configured Gemini client (implementation in data layer).
 */
interface VerseAiRecordingRepository {
    val isRecording: StateFlow<Boolean>

    fun startRecording()

    suspend fun stopRecordingAndValidate(currentTarget: String): Result<AiVerseValidationResult>
}
