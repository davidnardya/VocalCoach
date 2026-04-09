package org.davidnardya.vocalcoach.presentation.screens.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.domain.models.requirementForStage
import org.davidnardya.vocalcoach.domain.models.resolveStageProgression
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.game.PenaltyResult
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase

data class StageRoadmapItem(
    val stage: TrainingStage,
    val isCurrent: Boolean,
    val isUnlocked: Boolean,
    val isMaxUnlocked: Boolean,
    val requirementText: String?,
)

data class WelcomeRoadmapState(
    val currentStage: TrainingStage = TrainingStage.Stage1,
    val maxUnlockedStage: TrainingStage = TrainingStage.Stage1,
    val progressInCurrentStage: Float = 0f,
    val items: List<StageRoadmapItem> = emptyList(),
)

class WelcomeViewModel(
    private val repository: ScoreRepository,
    private val streakManagementUseCase: StreakManagementUseCase,
) : ViewModel() {
    private val _selectedStage = MutableStateFlow<TrainingStage?>(null)


    private val _penaltyMessage = MutableStateFlow<String?>(null)
    val penaltyMessage: StateFlow<String?> = _penaltyMessage.asStateFlow()

    private val _milestoneBonusMessage = MutableStateFlow<String?>(null)
    val milestoneBonusMessage: StateFlow<String?> = _milestoneBonusMessage.asStateFlow()

    val playerStats: StateFlow<PlayerStats?> = repository.getPlayerStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val roadmapState: StateFlow<WelcomeRoadmapState> = combine(
        playerStats,
        _selectedStage,
    ) { stats, selected ->
        if (stats == null) {
            return@combine WelcomeRoadmapState()
        }
        val progression = resolveStageProgression(
            level = stats.level,
            preferredStage = selected,
        )
        val items = TrainingStage.entries.map { stage ->
            val isUnlocked = stage.ordinal <= progression.maxUnlockedStage.ordinal
            val requirementText = if (isUnlocked) {
                null
            } else {
                val req = requirementForStage(stage)
                "Unlock at Level ${req.minLevel}"
            }
            StageRoadmapItem(
                stage = stage,
                isCurrent = stage == progression.currentStage,
                isUnlocked = isUnlocked,
                isMaxUnlocked = stage == progression.maxUnlockedStage,
                requirementText = requirementText,
            )
        }
        WelcomeRoadmapState(
            currentStage = progression.currentStage,
            maxUnlockedStage = progression.maxUnlockedStage,
            progressInCurrentStage = progression.progressInCurrentStage,
            items = items,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WelcomeRoadmapState(),
    )

    init {
        viewModelScope.launch {
            when (val r = streakManagementUseCase.runStartupCheck()) {
                is PenaltyResult.Applied -> _penaltyMessage.value = r.message
                is PenaltyResult.None -> Unit
            }
        }

        viewModelScope.launch {
            streakManagementUseCase.milestoneBonusEvents.collect { bonus ->
                _milestoneBonusMessage.value =
                    "LEGENDARY STREAK! +${bonus.bonusPoints} MILESTONE BONUS!"
            }
        }
    }

    fun consumePenaltyMessage() {
        _penaltyMessage.value = null
    }

    fun consumeMilestoneBonusMessage() {
        _milestoneBonusMessage.value = null
    }

    fun selectStage(stage: TrainingStage) {
        _selectedStage.value = stage
    }

    fun setTestProgress(scoreInput: Int, levelInput: Int) {
        val safeScore = scoreInput.coerceAtLeast(0).toDouble()
        val safeLevel = levelInput.coerceAtLeast(1)
        val minScoreFromLevel = ((safeLevel - 1) * 10).toDouble()
        val effectiveScore = maxOf(safeScore, minScoreFromLevel)

        viewModelScope.launch {
            repository.updateStats { stats ->
                stats.copy(totalScore = effectiveScore)
            }
        }
    }
}
