package org.davidnardya.vocalcoach.domain.usecase.audio

import kotlinx.datetime.Clock
import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

class HandleGrowlDetectionUseCase(
    private val repository: ScoreRepository
) {
    private var lastDetectionTime: Long = 0
    private val cooldownMs = 1500L

    suspend operator fun invoke(snapshot: ClassificationSnapshot) {
        if (snapshot.topLabel.contains("Growl", ignoreCase = true) && snapshot.topScore > 0.5f) {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            if (currentTime - lastDetectionTime > cooldownMs) {
                repository.incrementScore()
                lastDetectionTime = currentTime
            }
        }
    }
}
