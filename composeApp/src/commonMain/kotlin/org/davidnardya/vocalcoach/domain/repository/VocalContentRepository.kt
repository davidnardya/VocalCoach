package org.davidnardya.vocalcoach.domain.repository

import org.davidnardya.vocalcoach.domain.models.TrainingStage

interface VocalContentRepository {
    suspend fun getTargets(stage: TrainingStage): List<String>
}
