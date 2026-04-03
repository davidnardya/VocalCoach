package org.davidnardya.vocalcoach.domain.usecase.audio

expect class TranscriptionService {
    suspend fun transcribe(audioBuffer: FloatArray): String
}
