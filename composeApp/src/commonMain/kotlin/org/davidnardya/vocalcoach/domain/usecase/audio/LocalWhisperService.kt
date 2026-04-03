package org.davidnardya.vocalcoach.domain.usecase.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface for Local Whisper-Tiny transcription.
 * Implementation would typically wrap Whisper.cpp via JNI/C-Interop.
 */
interface LocalWhisperService {
    suspend fun transcribe(audioBuffer: FloatArray): String
}

/**
 * Mock/Stub implementation for the purpose of this architecture.
 * In a real scenario, this would call into the Whisper.cpp KMP library.
 */
class LocalWhisperServiceImpl : LocalWhisperService {
    override suspend fun transcribe(audioBuffer: FloatArray): String = withContext(Dispatchers.Default) {
        // Here we would invoke whisper_full_parallel or similar
        // For now, returning an empty string to be handled by the validator
        ""
    }
}
