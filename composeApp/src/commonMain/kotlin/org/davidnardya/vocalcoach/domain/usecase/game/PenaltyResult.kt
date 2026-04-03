package org.davidnardya.vocalcoach.domain.usecase.game

sealed class PenaltyResult {
    data object None : PenaltyResult()

    data class Applied(
        val pointsDeducted: Long,
        val streakReset: Boolean,
        val message: String,
    ) : PenaltyResult()
}
