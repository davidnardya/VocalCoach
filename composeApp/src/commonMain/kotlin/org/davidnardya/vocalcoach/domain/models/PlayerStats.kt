package org.davidnardya.vocalcoach.domain.models

data class PlayerStats(
    val totalScore: Int,
    val level: Int
) {
    companion object {
        fun fromScore(score: Int): PlayerStats {
            return PlayerStats(
                totalScore = score,
                level = (score / 10) + 1
            )
        }
    }
}
