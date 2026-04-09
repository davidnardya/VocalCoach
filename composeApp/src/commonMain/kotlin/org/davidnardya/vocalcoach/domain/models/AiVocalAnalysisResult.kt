package org.davidnardya.vocalcoach.domain.models

/**
 * Result of extreme-vocal validation (Gemini): technique + rough lyric match, not production quality.
 */
data class AiVocalAnalysisResult(
    val isSuccessful: Boolean,
    /** e.g. scream, growl — from the model. */
    val techniqueDetected: String,
    val feedback: String,
    /** 0.0–1.0 for UI metering; derived when the API omits a numeric score. */
    val technicalScore: Double,
)
