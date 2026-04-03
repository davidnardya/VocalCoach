package org.davidnardya.vocalcoach.domain.usecase.audio

import org.davidnardya.vocalcoach.core.util.FuzzyMatcher
import org.davidnardya.vocalcoach.domain.models.ChallengeType
import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.models.VocalChallenge
import org.davidnardya.vocalcoach.domain.models.VocalValidationResult
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase

/** Growl confidence threshold for Step 1 Technique check. */
private const val GROWL_GATE_THRESHOLD = 0.45f

class VocalValidatorImpl(
    private val transcriptionService: TranscriptionService,
    private val whisperService: LocalWhisperService,
    private val calculatePointsUseCase: CalculatePointsUseCase,
) : VocalValidator {

    override suspend fun validate(
        snapshot: ClassificationSnapshot,
        challenge: VocalChallenge,
        audioBuffer: FloatArray,
        stats: PlayerStats,
    ): VocalValidationResult {
        val points = calculatePointsUseCase.pointsForGrowl(stats.levelsGainedToday).toInt()

        // STEP 1: TECHNIQUE (LiteRT)
        val isGrowlSuccess = snapshot.topLabel.contains("Growl", ignoreCase = true) &&
            snapshot.topScore >= GROWL_GATE_THRESHOLD

        if (!isGrowlSuccess) {
            return VocalValidationResult.Failure(
                reason = "Not a growl (confidence ${snapshot.topScore})",
                heardTranscription = null,
            )
        }

        // DEBUG OVERRIDE
        if (stats.debugDisableContentCheck) {
            println("DEBUG: Content check disabled. Auto-success for growl.")
            return VocalValidationResult.Success(points = points, heardTranscription = "debug:skipped")
        }

        // GROWL-ONLY CHALLENGES
        if (challenge.type == ChallengeType.GROWL) {
            return VocalValidationResult.Success(points = points, heardTranscription = null)
        }

        // STEP 2: CONTENT (Whisper / STT)
        val bufferForStt = audioBuffer.copyOf()
        
        // Choose engine based on challenge type (Era 3+ uses Local Whisper)
        val transcription = if (challenge.type == ChallengeType.WORD || 
            challenge.type == ChallengeType.LYRICAL || 
            challenge.type == ChallengeType.MULTI_VERSE) {
            whisperService.transcribe(bufferForStt).trim()
        } else {
            transcriptionService.transcribe(bufferForStt).trim()
        }

        return when (challenge.type) {
            ChallengeType.VOWEL -> validateVowel(challenge, transcription, points, bufferForStt)
            ChallengeType.WORD -> validateFuzzy(challenge, transcription, points, threshold = 0.6f)
            ChallengeType.LYRICAL, ChallengeType.MULTI_VERSE -> 
                validateFuzzy(challenge, transcription, points, threshold = 0.5f)
            else -> VocalValidationResult.Failure("Unknown challenge type", transcription)
        }
    }

    private fun validateVowel(
        challenge: VocalChallenge,
        transcription: String,
        points: Int,
        buffer: FloatArray
    ): VocalValidationResult {
        if (isTranscriptionMatch(challenge.target, transcription)) {
            return VocalValidationResult.Success(points = points, heardTranscription = transcription)
        }
        
        // Fallback to FrequencyHeuristic for Vowels
        val detectedVowel = frequencyHeuristic(buffer)
        if (detectedVowel == challenge.target.uppercase()) {
            return VocalValidationResult.Success(points = points, heardTranscription = "heuristic:$detectedVowel")
        }

        return VocalValidationResult.PartialSuccess(
            message = "Technique 100%! Now shape that [${challenge.target}] vowel.",
            heardTranscription = transcription
        )
    }

    private fun validateFuzzy(
        challenge: VocalChallenge,
        transcription: String,
        points: Int,
        threshold: Float
    ): VocalValidationResult {
        if (transcription.isEmpty()) {
            return VocalValidationResult.Failure("AI heard silence. Keep pushing!", transcription)
        }

        val similarity = FuzzyMatcher.similarity(challenge.target, transcription)
        println("DEBUG: Fuzzy Match [${challenge.target}] vs [$transcription] = $similarity")

        return if (similarity >= threshold) {
            VocalValidationResult.Success(points = points, heardTranscription = transcription)
        } else {
            val feedback = if (challenge.type == ChallengeType.WORD) "Close! Try to enunciate." else "Lyrics mismatch. Keep the rhythm!"
            VocalValidationResult.PartialSuccess(message = feedback, heardTranscription = transcription)
        }
    }

    private fun frequencyHeuristic(buffer: FloatArray): String? {
        if (buffer.isEmpty()) return null
        val sampleRate = 16000f
        var zeroCrossings = 0
        for (i in 1 until buffer.size) {
            if ((buffer[i-1] > 0 && buffer[i] <= 0) || (buffer[i-1] < 0 && buffer[i] >= 0)) {
                zeroCrossings++
            }
        }
        val dominantFreq = (zeroCrossings * sampleRate) / (2 * buffer.size)
        return when {
            dominantFreq in 200.0..600.0 -> if (dominantFreq < 400) "U" else "O"
            dominantFreq in 700.0..1300.0 -> "A"
            else -> null
        }
    }

    private fun isTranscriptionMatch(targetVowel: String, transcription: String): Boolean {
        val n = transcription.lowercase().trimEnd('.', '!', '?')
        if (n.isEmpty()) return false
        return when (targetVowel.uppercase()) {
            "A" -> listOf("ah", "ahhh", "eye", "ay", "a", "i").any { n.contains(it) }
            "O" -> listOf("o", "oh", "who", "ow", "0").any { n.contains(it) }
            "U" -> listOf("u", "you", "eww", "ew").any { n.contains(it) }
            else -> n.contains(targetVowel.lowercase())
        }
    }
}
