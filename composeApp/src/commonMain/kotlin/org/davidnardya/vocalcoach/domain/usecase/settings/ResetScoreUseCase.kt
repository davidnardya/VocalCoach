package org.davidnardya.vocalcoach.domain.usecase.settings

import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

class ResetScoreUseCase(private val repository: ScoreRepository) {
    suspend operator fun invoke() = repository.resetScore()
}
