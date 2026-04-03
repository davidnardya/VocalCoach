package org.davidnardya.vocalcoach.domain.models

import androidx.compose.ui.graphics.Color

enum class ChallengeType(val color: Color) {
    GROWL(Color(0xFFB22222)), // BloodRed
    VOWEL(Color(0xFFA020F0)), // Purple
    WORD(Color(0xFF00FFFF)), // Cyan
    LYRICAL(Color(0xFF00FF00)), // Green
    MULTI_VERSE(Color(0xFFFFD700)), // Legendary Gold — full song / multi-verse
}

data class VocalChallenge(
    val id: String,
    val type: ChallengeType,
    val target: String,
    val description: String
)
