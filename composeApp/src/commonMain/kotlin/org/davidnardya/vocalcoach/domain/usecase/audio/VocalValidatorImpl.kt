package org.davidnardya.vocalcoach.domain.usecase.audio

import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.models.VocalChallenge
import org.davidnardya.vocalcoach.domain.models.VocalValidationResult
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase

/** 
 * Multi-Class Teachable Machine Model Configuration.
 * Global threshold for success is 90%.
 */
private const val GLOBAL_THRESHOLD = 0.90f
private const val CLEAN_VOCALS_LABEL = "Clean_Vocals"

class VocalValidatorImpl(
    private val calculatePointsUseCase: CalculatePointsUseCase,
) : VocalValidator {

    override suspend fun validate(
        snapshot: ClassificationSnapshot,
        challenge: VocalChallenge,
        audioBuffer: FloatArray,
        stats: PlayerStats,
    ): VocalValidationResult {
        val points = calculatePointsUseCase.pointsForGrowl(stats.levelsGainedToday).toInt()
        val topLabel = snapshot.topLabel
        val topScore = snapshot.topScore

        // ANTI-CHEAT: Clean vocals cannot count as growls
        if (topLabel == CLEAN_VOCALS_LABEL) {
            return VocalValidationResult.Failure(
                reason = "That's a clean vocal! Add more grit/distortion to count as a growl.",
                heardTranscription = null
            )
        }

        // GLOBAL THRESHOLD CHECK
        if (topScore < GLOBAL_THRESHOLD) {
            val percentage = (topScore * 100).toInt()
            return VocalValidationResult.Failure(
                reason = "Technique too weak ($percentage%). Aim for 90%!",
                heardTranscription = null
            )
        }

        val isGrowlLabel = topLabel.startsWith("Growl_")
        if (!isGrowlLabel) {
            return VocalValidationResult.Failure(
                reason = "No growl detected.",
                heardTranscription = null
            )
        }

        // ERA-BASED LOGIC
        return when {
            stats.level <= 50 -> {
                // ERA 1: The Primal Void (Levels 1-50)
                // Reward any successful growl technique regardless of vowel shape.
                VocalValidationResult.Success(points = points, heardTranscription = topLabel.substringAfter("Growl_"))
            }
            stats.level <= 100 -> {
                // ERA 2: The Resonant Chamber (Levels 51-100)
                // Precision matters: Must match the target vowel.
                val detectedVowel = topLabel.substringAfter("Growl_")
                val targetVowel = challenge.target.uppercase()

                if (detectedVowel == targetVowel) {
                    VocalValidationResult.Success(points = points, heardTranscription = detectedVowel)
                } else {
                    VocalValidationResult.PartialSuccess(
                        message = "Wrong shape! You're hitting an [$detectedVowel], try for [$targetVowel].",
                        heardTranscription = detectedVowel
                    )
                }
            }
            else -> {
                // Fallback for Era 3+ (if any), currently following Era 2 logic or extending.
                VocalValidationResult.Success(points = points, heardTranscription = topLabel.substringAfter("Growl_"))
            }
        }
    }
}
