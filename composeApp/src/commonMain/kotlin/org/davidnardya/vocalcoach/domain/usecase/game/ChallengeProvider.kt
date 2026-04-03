package org.davidnardya.vocalcoach.domain.usecase.game

import org.davidnardya.vocalcoach.domain.models.ChallengeType
import org.davidnardya.vocalcoach.domain.models.VocalChallenge
import org.davidnardya.vocalcoach.domain.models.VOCAL_LEVELS_PER_ERA

class ChallengeProvider {
    private val vowels = listOf("A", "O", "E", "I", "U")
    private val words = listOf("Fire", "Death", "Chaos", "Metal", "Scream", "Void", "Power")
    private val verses = listOf(
        "Into the abyss we fall",
        "Silence is the loudest cry",
        "Burning wings in the night",
        "Echoes of a broken soul",
    )
    private val multiVerses = listOf(
        "Into the void we march through fire and ice",
        "The ancient verse awakens chains of fate tonight",
        "Scream the chorus till the heavens split in two",
    )

    fun getChallenge(level: Int, challengeIndex: Int): VocalChallenge {
        val era = (level.coerceAtLeast(1) - 1) / VOCAL_LEVELS_PER_ERA
        return when (era) {
            0 -> VocalChallenge(
                id = "growl_primal_${challengeIndex % 1000}",
                type = ChallengeType.GROWL,
                target = "Growl",
                description = "Raw Growl Technique — unleash a primal growl",
            )
            1 -> {
                val target = vowels[challengeIndex % vowels.size]
                VocalChallenge(
                    id = "vowel_$target",
                    type = ChallengeType.VOWEL,
                    target = target,
                    description = "Vowel Articulation — master the '$target' vowel growl",
                )
            }
            2 -> {
                val target = words[challengeIndex % words.size]
                VocalChallenge(
                    id = "word_$target",
                    type = ChallengeType.WORD,
                    target = target,
                    description = "Specific Word Attacks — hit the word: $target",
                )
            }
            3 -> {
                val target = verses[challengeIndex % verses.size]
                VocalChallenge(
                    id = "lyrical_${challengeIndex % verses.size}",
                    type = ChallengeType.LYRICAL,
                    target = target,
                    description = "Full Lyrical Sentences — $target",
                )
            }
            else -> {
                val target = multiVerses[challengeIndex % multiVerses.size]
                VocalChallenge(
                    id = "multiverse_${challengeIndex % multiVerses.size}",
                    type = ChallengeType.MULTI_VERSE,
                    target = target,
                    description = "Multi-verse Song Performance — $target",
                )
            }
        }
    }
}
