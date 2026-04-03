package org.davidnardya.vocalcoach.domain.models

/**
 * Outcome of [org.davidnardya.vocalcoach.domain.usecase.audio.VocalValidator.validate].
 */
sealed class VocalValidationResult {
    /**
     * Growl gate passed and transcription matched when required — scoring may proceed.
     * [points] is the score increment for this growl (hot-state multiplier already applied).
     */
    data class Success(
        val points: Int,
        val heardTranscription: String? = null,
    ) : VocalValidationResult()

    /** Growl technique passed, but vowel/word did not match (or empty STT for vowel). */
    data class PartialSuccess(
        val message: String,
        val heardTranscription: String? = null,
    ) : VocalValidationResult()

    /** Did not pass validation (wrong technique, bad match, etc.). */
    data class Failure(
        val reason: String,
        val heardTranscription: String? = null,
    ) : VocalValidationResult()
}
