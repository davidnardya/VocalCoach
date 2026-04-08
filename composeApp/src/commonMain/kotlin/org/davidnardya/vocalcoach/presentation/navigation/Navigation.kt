package org.davidnardya.vocalcoach.presentation.navigation

import kotlinx.serialization.Serializable
import org.davidnardya.vocalcoach.domain.models.TrainingStage

@Serializable
object WelcomeRoute

@Serializable
data class GameRoute(
    val stage: TrainingStage = TrainingStage.Stage1,
)

@Serializable
object SettingsRoute
