package org.davidnardya.vocalcoach.domain.usecase.audio

import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.models.VocalChallenge
import org.davidnardya.vocalcoach.domain.models.VocalValidationResult

interface VocalValidator {
    suspend fun validate(
        snapshot: ClassificationSnapshot,
        challenge: VocalChallenge,
        audioBuffer: FloatArray,
        stats: PlayerStats,
    ): VocalValidationResult
}
