package org.davidnardya.vocalcoach.presentation.screens.welcome

import org.davidnardya.vocalcoach.domain.models.VocalStage
import vocalcoach.composeapp.generated.resources.Res
import vocalcoach.composeapp.generated.resources.stage_the_harbinger
import vocalcoach.composeapp.generated.resources.stage_the_resonance
import vocalcoach.composeapp.generated.resources.stage_the_verse
import vocalcoach.composeapp.generated.resources.stage_the_void
import vocalcoach.composeapp.generated.resources.stage_the_word

val DefaultVocalRoadmapStages: List<VocalStage> = listOf(
    VocalStage(
        name = "The Primal Void",
        levels = 1..50,
        icon = Res.drawable.stage_the_void,
        description = "Objective: Raw Growl Technique",
    ),
    VocalStage(
        name = "The Resonant Chamber",
        levels = 51..100,
        icon = Res.drawable.stage_the_resonance,
        description = "Objective: Vowel Articulation (A, O, E, I, U)",
    ),
    VocalStage(
        name = "The Iron Word",
        levels = 101..150,
        icon = Res.drawable.stage_the_word,
        description = "Objective: Specific Word Attacks",
    ),
    VocalStage(
        name = "The Ancient Verse",
        levels = 151..200,
        icon = Res.drawable.stage_the_verse,
        description = "Objective: Full Lyrical Sentences",
    ),
    VocalStage(
        name = "The Harbinger",
        levels = 201..Int.MAX_VALUE,
        icon = Res.drawable.stage_the_harbinger,
        description = "Objective: Multi-verse Song Performance",
    ),
)
