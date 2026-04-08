package org.davidnardya.vocalcoach.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class TrainingStage(
    val title: String,
    val description: String,
) {
    Stage1(
        title = "Stage 1",
        description = "Make any growl",
    ),
    Stage2(
        title = "Stage 2",
        description = "Match a target vowel",
    ),
    Stage3(
        title = "Stage 3",
        description = "Growl a target word",
    ),
    Stage4(
        title = "Stage 4",
        description = "Growl a target sentence",
    ),
    Stage5(
        title = "Stage 5",
        description = "Growl a verse or multiple lines",
    ),
}

data class StageRequirement(
    val minTotalScore: Long,
    val minLevel: Int,
)

data class StageProgression(
    val currentStage: TrainingStage,
    val maxUnlockedStage: TrainingStage,
)

private val stageRequirements: Map<TrainingStage, StageRequirement> = mapOf(
    TrainingStage.Stage1 to StageRequirement(minTotalScore = 0L, minLevel = 1),
    TrainingStage.Stage2 to StageRequirement(minTotalScore = 50L, minLevel = 5),
    TrainingStage.Stage3 to StageRequirement(minTotalScore = 150L, minLevel = 8),
    TrainingStage.Stage4 to StageRequirement(minTotalScore = 300L, minLevel = 12),
    TrainingStage.Stage5 to StageRequirement(minTotalScore = 500L, minLevel = 16),
)

fun resolveStageProgression(
    totalScore: Long,
    level: Int,
    preferredStage: TrainingStage?,
): StageProgression {
    val maxUnlocked = TrainingStage.entries
        .filter { stage ->
            val requirement = stageRequirements.getValue(stage)
            totalScore >= requirement.minTotalScore && level >= requirement.minLevel
        }
        .maxByOrNull { it.ordinal }
        ?: TrainingStage.Stage1

    val current = when {
        preferredStage == null -> maxUnlocked
        preferredStage.ordinal <= maxUnlocked.ordinal -> preferredStage
        else -> maxUnlocked
    }

    return StageProgression(currentStage = current, maxUnlockedStage = maxUnlocked)
}

fun requirementForStage(stage: TrainingStage): StageRequirement = stageRequirements.getValue(stage)
