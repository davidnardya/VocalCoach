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
    val minLevel: Int,
)

data class StageProgression(
    val currentStage: TrainingStage,
    val maxUnlockedStage: TrainingStage,
    val progressInCurrentStage: Float, // 0.0 to 1.0, representing 5 levels
)

private val stageRequirements: Map<TrainingStage, StageRequirement> = mapOf(
    TrainingStage.Stage1 to StageRequirement(minLevel = 1),
    TrainingStage.Stage2 to StageRequirement(minLevel = 51),
    TrainingStage.Stage3 to StageRequirement(minLevel = 101),
    TrainingStage.Stage4 to StageRequirement(minLevel = 151),
    TrainingStage.Stage5 to StageRequirement(minLevel = 201),
)

fun resolveStageProgression(
    level: Int,
    preferredStage: TrainingStage?,
): StageProgression {
    val maxUnlocked = TrainingStage.entries
        .filter { stage ->
            val requirement = stageRequirements.getValue(stage)
            level >= requirement.minLevel
        }
        .maxByOrNull { it.ordinal }
        ?: TrainingStage.Stage1

    val current = when {
        preferredStage == null -> maxUnlocked
        preferredStage.ordinal <= maxUnlocked.ordinal -> preferredStage
        else -> maxUnlocked
    }

    // Calculate progress (dots logic)
    // 5 dots total. Each dot = 1 level (10 points).
    // Progress 1.0 means 5 levels gained in this stage beyond the minimum requirement.
    val startLevel = stageRequirements.getValue(maxUnlocked).minLevel
    val levelsIntoStage = (level - startLevel).coerceAtLeast(0)
    val progress = (levelsIntoStage.toFloat() / 5f).coerceIn(0f, 1f)

    return StageProgression(
        currentStage = current,
        maxUnlockedStage = maxUnlocked,
        progressInCurrentStage = progress
    )
}

fun requirementForStage(stage: TrainingStage): StageRequirement = stageRequirements.getValue(stage)
