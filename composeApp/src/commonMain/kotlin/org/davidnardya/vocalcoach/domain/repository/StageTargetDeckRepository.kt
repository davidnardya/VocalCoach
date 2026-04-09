package org.davidnardya.vocalcoach.domain.repository

import org.davidnardya.vocalcoach.domain.models.TrainingStage

/**
 * Persistent deck state for stage targets.
 * Stores which indices were already consumed for each stage.
 */
interface StageTargetDeckRepository {
    suspend fun getUsedIndices(stage: TrainingStage): Set<Int>

    suspend fun saveUsedIndices(stage: TrainingStage, usedIndices: Set<Int>)
}
