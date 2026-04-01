package org.davidnardya.vocalcoach.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.domain.AudioClassificationUseCase
import org.davidnardya.vocalcoach.domain.LabelScore

data class AudioClassificationUiState(
    val isListening: Boolean = false,
    val topLabel: String = "—",
    val topScore: Float = 0f,
    val ranked: List<LabelScore> = emptyList(),
    val message: String? = null,
)

class AudioClassificationViewModel(
    private val useCase: AudioClassificationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioClassificationUiState())
    val state: StateFlow<AudioClassificationUiState> = _state.asStateFlow()

    private var cachedModelBytes: ByteArray? = null

    fun onStartStopClicked(
        permissionGranted: Boolean,
        modelLoader: suspend () -> ByteArray,
    ) {
        if (!permissionGranted) {
            _state.update { it.copy(message = "Microphone permission is required.") }
            return
        }
        if (_state.value.isListening) {
            useCase.stop()
            _state.update { it.copy(isListening = false, message = null) }
            return
        }
        viewModelScope.launch {
            try {
                val bytes = cachedModelBytes ?: modelLoader().also { cachedModelBytes = it }
                _state.update {
                    it.copy(topLabel = "—", topScore = 0f, ranked = emptyList(), message = null)
                }
                useCase.start(viewModelScope, bytes) { snap ->
                    _state.update { s ->
                        if (snap.topScore < MIN_DISPLAY_CONFIDENCE) {
                            s
                        } else {
                            s.copy(
                                topLabel = snap.topLabel,
                                topScore = snap.topScore,
                                ranked = snap.ranked,
                            )
                        }
                    }
                }
                _state.update { it.copy(isListening = true, message = null) }
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        isListening = false,
                        message = e.message ?: "Could not start classification",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        useCase.stop()
        super.onCleared()
    }

    private companion object {
        /** Only update label/score when the model is at least this confident (90%). */
        const val MIN_DISPLAY_CONFIDENCE = 0.9f
    }
}
