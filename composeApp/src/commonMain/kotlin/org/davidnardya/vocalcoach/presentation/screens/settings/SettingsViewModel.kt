package org.davidnardya.vocalcoach.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.domain.models.NotificationSettings
import org.davidnardya.vocalcoach.domain.usecase.settings.GetNotificationSettingsUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.ResetScoreUseCase
import org.davidnardya.vocalcoach.domain.usecase.settings.SaveSettingsUseCase

class SettingsViewModel(
    private val getSettingsUseCase: GetNotificationSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val resetScoreUseCase: ResetScoreUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationSettings())
    val state: StateFlow<NotificationSettings> = _state.asStateFlow()

    private val _draftState = MutableStateFlow(NotificationSettings())
    val draftState: StateFlow<NotificationSettings> = _draftState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _state.value = settings
                _draftState.value = settings
            }
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _draftState.value = _draftState.value.copy(enabled = enabled)
    }

    fun onTimeChanged(hour: Int, minute: Int) {
        _draftState.value = _draftState.value.copy(hour = hour, minute = minute)
    }

    fun onIntervalChanged(hours: Long) {
        _draftState.value = _draftState.value.copy(intervalHours = hours)
    }

    fun onSaveClicked(hasNotificationPermission: Boolean) {
        if (_draftState.value.enabled && !hasNotificationPermission) {
            viewModelScope.launch {
                _events.emit(SettingsEvent.ShowPermissionExplanation)
            }
            return
        }

        viewModelScope.launch {
            saveSettingsUseCase(_draftState.value)
            _state.value = _draftState.value
            _events.emit(SettingsEvent.ShowSnackbar("Settings saved"))
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            resetScoreUseCase()
            _events.emit(SettingsEvent.ShowSnackbar("Progress reset successfully"))
        }
    }
}

sealed interface SettingsEvent {
    data class ShowSnackbar(val message: String) : SettingsEvent
    data object ShowPermissionExplanation : SettingsEvent
}
