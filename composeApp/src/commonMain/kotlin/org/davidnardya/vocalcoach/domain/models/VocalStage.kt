package org.davidnardya.vocalcoach.domain.models

import org.jetbrains.compose.resources.DrawableResource

data class VocalStage(
    val name: String,
    val levels: IntRange,
    val icon: DrawableResource,
    val description: String,
)

fun VocalStage.isLocked(playerLevel: Int): Boolean = playerLevel < levels.first

fun VocalStage.isCompleted(playerLevel: Int): Boolean =
    when {
        levels.last == Int.MAX_VALUE -> false
        else -> playerLevel > levels.last
    }

fun VocalStage.isCurrent(playerLevel: Int): Boolean =
    !isLocked(playerLevel) && !isCompleted(playerLevel)
