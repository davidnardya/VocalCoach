package org.davidnardya.vocalcoach.domain.usecase.game

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatePointsUseCaseTest {

    private val useCase = CalculatePointsUseCase()

    @Test
    fun progressing_lowMidHigh_tiers() {
        assertEquals(1.0, useCase.pointsForHit(activeStage = 3, maxUnlockedStage = 3, hotStreakCount = 1))
        assertEquals(2.0, useCase.pointsForHit(activeStage = 3, maxUnlockedStage = 3, hotStreakCount = 10))
        assertEquals(3.0, useCase.pointsForHit(activeStage = 3, maxUnlockedStage = 3, hotStreakCount = 20))
    }

    @Test
    fun practicing_lowMidHigh_tiers() {
        assertEquals(0.5, useCase.pointsForHit(activeStage = 2, maxUnlockedStage = 5, hotStreakCount = 3))
        assertEquals(1.0, useCase.pointsForHit(activeStage = 2, maxUnlockedStage = 5, hotStreakCount = 8))
        assertEquals(2.0, useCase.pointsForHit(activeStage = 2, maxUnlockedStage = 5, hotStreakCount = 16))
    }

    @Test
    fun preview_matches_pointsForHit_with_incremented_streak() {
        assertEquals(
            useCase.pointsForHit(2, 2, 6),
            useCase.previewPointsForNextHit(2, 2, sessionStreakBeforeHit = 5),
        )
    }
}
