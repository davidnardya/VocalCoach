package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.domain.models.VocalChallenge
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.audio.AudioClassificationUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.GrowlUiFeedback
import org.davidnardya.vocalcoach.domain.usecase.audio.HandleGrowlDetectionUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.LevelTrackerUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.ChallengeProvider
import org.davidnardya.vocalcoach.domain.usecase.game.LevelUpCelebration
import org.davidnardya.vocalcoach.domain.usecase.game.PenaltyResult
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase

data class AudioClassificationUiState(
    val isRecording: Boolean = false,
    val currentLabel: String = "—",
    val totalScore: Long = 0L,
    val currentLevel: Int = 1,
    val levelsGainedToday: Int = 0,
    val streakCount: Int = 0,
    val scoreMultiplier: Double = 1.0,
    val message: String? = null,
    val showExitConfirmation: Boolean = false,
    val levelUpCelebration: LevelUpCelebration? = null,
    val isMilestoneLevelUp: Boolean = false,
    val currentChallenge: VocalChallenge? = null,
    /** Last STT text (Era 2+) for calibration when level > 50. */
    val aiHeardTranscription: String? = null,
    val isAnalyzing: Boolean = false,
)

class AudioClassificationViewModel(
    private val audioUseCase: AudioClassificationUseCase,
    private val repository: ScoreRepository,
    levelTrackerUseCase: LevelTrackerUseCase,
    private val handleGrowlDetectionUseCase: HandleGrowlDetectionUseCase,
    private val calculatePointsUseCase: CalculatePointsUseCase,
    private val streakManagementUseCase: StreakManagementUseCase,
    private val challengeProvider: ChallengeProvider,
) : ViewModel() {

    private val _internalState = MutableStateFlow(
        InternalState(isRecording = false, currentLabel = "—"),
    )

    private val _levelUpCelebration = MutableStateFlow<LevelUpCelebration?>(null)
    private var clearLevelUpOverlayJob: Job? = null

    val milestoneBonusEvents get() = streakManagementUseCase.milestoneBonusEvents
    val levelUpCelebrations get() = streakManagementUseCase.levelUpCelebrations

    private val _penaltyEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val penaltyEvents = _penaltyEvents.asSharedFlow()

    init {
        levelTrackerUseCase.monitorLevelUps()
            .launchIn(viewModelScope)

        viewModelScope.launch {
            when (val r = streakManagementUseCase.runStartupCheck()) {
                is PenaltyResult.Applied -> _penaltyEvents.emit(r.message)
                is PenaltyResult.None -> Unit
            }
        }

        viewModelScope.launch {
            streakManagementUseCase.levelUpCelebrations.collect { celebration ->
                _levelUpCelebration.value = celebration
                clearLevelUpOverlayJob?.cancel()
                clearLevelUpOverlayJob = viewModelScope.launch {
                    delay(1700)
                    _levelUpCelebration.value = null
                }
            }
        }
    }

    val multiplier: StateFlow<Double> = repository.getPlayerStats()
        .map { stats -> calculatePointsUseCase.computeMultiplier(stats.levelsGainedToday) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0,
        )

    val state: StateFlow<AudioClassificationUiState> = combine(
        _internalState,
        repository.getPlayerStats(),
        _levelUpCelebration,
        multiplier,
    ) { internal, stats, levelUpCelebration, mult ->
        val challenge = challengeProvider.getChallenge(stats.level, stats.currentChallengeIndex)
        val milestone = levelUpCelebration?.milestoneBonus
        AudioClassificationUiState(
            isRecording = internal.isRecording,
            currentLabel = internal.currentLabel,
            totalScore = stats.totalScore,
            currentLevel = stats.level,
            levelsGainedToday = stats.levelsGainedToday,
            streakCount = stats.streakCount,
            scoreMultiplier = mult,
            message = internal.message,
            showExitConfirmation = internal.showExitConfirmation,
            levelUpCelebration = levelUpCelebration,
            isMilestoneLevelUp = milestone != null,
            currentChallenge = challenge,
            aiHeardTranscription = internal.aiHeardTranscription,
            isAnalyzing = internal.isAnalyzing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AudioClassificationUiState(),
    )

    private var cachedModelBytes: ByteArray? = null
    private var isProcessingGrowl = false

    fun onStartStopClicked(
        permissionGranted: Boolean,
        modelLoader: suspend () -> ByteArray,
    ) {
        if (!permissionGranted) {
            _internalState.update { it.copy(message = "Microphone permission is required.") }
            return
        }
        if (_internalState.value.isRecording) {
            audioUseCase.stop()
            _internalState.update { it.copy(isRecording = false, message = null, isAnalyzing = false) }
            return
        }
        viewModelScope.launch {
            try {
                val bytes = cachedModelBytes ?: modelLoader().also { cachedModelBytes = it }
                _internalState.update {
                    it.copy(currentLabel = "—", message = null, aiHeardTranscription = null, isAnalyzing = false)
                }
                audioUseCase.start(viewModelScope, bytes) { snap ->
                    _internalState.update { s ->
                        s.copy(currentLabel = snap.topLabel)
                    }
                    
                    // Only process if not already busy with a heavy transcription (Whisper)
                    if (isProcessingGrowl) return@start
                    
                    viewModelScope.launch {
                        val currentStats = repository.getPlayerStats().first()
                        
                        // We set isAnalyzing only if we think a growl happened and we are in Era 2+
                        val isLikelyGrowl = snap.topLabel.contains("Growl", ignoreCase = true) && snap.topScore >= 0.45f
                        if (isLikelyGrowl && currentStats.level >= 51) {
                            _internalState.update { it.copy(isAnalyzing = true) }
                            isProcessingGrowl = true
                        }

                        val feedback: GrowlUiFeedback = handleGrowlDetectionUseCase(snap, currentStats)
                        
                        _internalState.update { prev ->
                            prev.copy(
                                message = when {
                                    feedback.partialMessage != null -> feedback.partialMessage
                                    feedback.scoredPoints -> null
                                    else -> prev.message
                                },
                                aiHeardTranscription = feedback.aiHeard ?: prev.aiHeardTranscription,
                                isAnalyzing = false
                            )
                        }
                        isProcessingGrowl = false
                    }
                }
                _internalState.update { it.copy(isRecording = true, message = null) }
            } catch (e: Throwable) {
                _internalState.update {
                    it.copy(
                        isRecording = false,
                        message = e.message ?: "Could not start classification",
                    )
                }
            }
        }
    }

    fun onBackRequested() {
        if (_internalState.value.isRecording) {
            _internalState.update { it.copy(showExitConfirmation = true) }
        }
    }

    fun onDismissExitConfirmation() {
        _internalState.update { it.copy(showExitConfirmation = false) }
    }

    override fun onCleared() {
        audioUseCase.stop()
        super.onCleared()
    }

    private data class InternalState(
        val isRecording: Boolean,
        val currentLabel: String,
        val message: String? = null,
        val showExitConfirmation: Boolean = false,
        val aiHeardTranscription: String? = null,
        val isAnalyzing: Boolean = false,
    )
}
