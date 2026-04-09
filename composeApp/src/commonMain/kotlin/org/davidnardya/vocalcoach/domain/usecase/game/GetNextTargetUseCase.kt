package org.davidnardya.vocalcoach.domain.usecase.game

import kotlin.random.Random
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.domain.repository.StageTargetDeckRepository
import org.davidnardya.vocalcoach.domain.repository.VocalContentRepository

/**
 * Draws one target from a persistent shuffled deck.
 * No target repeats until every index in the stage deck has been used.
 */
class GetNextTargetUseCase(
    private val contentRepository: VocalContentRepository,
    private val deckRepository: StageTargetDeckRepository,
) {
    suspend operator fun invoke(stage: TrainingStage): String {
        val masterList = contentRepository.getTargets(stage)
        if (masterList.isEmpty()) return ""

        val used = deckRepository.getUsedIndices(stage)
        val effectiveUsed = if (used.size >= masterList.size) {
            emptySet()
        } else {
            used
        }

        val remainingIndices = masterList.indices.filter { it !in effectiveUsed }
        val selectedIndex = remainingIndices.random(Random.Default)
        val updatedUsed = effectiveUsed + selectedIndex

        deckRepository.saveUsedIndices(stage, updatedUsed)
        return masterList[selectedIndex]
    }
}
