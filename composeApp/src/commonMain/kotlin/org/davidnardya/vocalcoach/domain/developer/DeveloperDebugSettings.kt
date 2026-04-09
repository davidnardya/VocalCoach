package org.davidnardya.vocalcoach.domain.developer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.davidnardya.vocalcoach.domain.models.TrainingStage

/**
 * In-memory developer toggles (secret menu). Not persisted.
 */
object DeveloperDebugSettings {
    private val _maxUnlockedStageBoost = MutableStateFlow<TrainingStage?>(null)
    val maxUnlockedStageBoost: StateFlow<TrainingStage?> = _maxUnlockedStageBoost.asStateFlow()

    fun setMaxUnlockedStageBoost(stage: TrainingStage?) {
        _maxUnlockedStageBoost.value = stage
    }

    private val _mockGeminiSuccess = MutableStateFlow(false)
    val mockGeminiSuccess: StateFlow<Boolean> = _mockGeminiSuccess.asStateFlow()

    fun setMockGeminiSuccess(enabled: Boolean) {
        _mockGeminiSuccess.value = enabled
    }
}
