package org.davidnardya.vocalcoach.presentation.screens.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.game.PenaltyResult
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase

class WelcomeViewModel(
    repository: ScoreRepository,
    private val streakManagementUseCase: StreakManagementUseCase,
) : ViewModel() {

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
}
