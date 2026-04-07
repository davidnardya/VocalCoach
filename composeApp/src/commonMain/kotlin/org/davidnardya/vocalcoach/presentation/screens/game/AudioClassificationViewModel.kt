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
import org.davidnardya.vocalcoach.domain.models.LabelScore
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
    val aiHeardTranscription: String? = null,
    val isAnalyzing: Boolean = false,
    /** Precision Meter: 0.0 to 1.0 based on topScore. Success threshold is 0.90. */
    val precisionScore: Float = 0f,
    /** Top 3 detected labels for debug overlay. */
    val top3Labels: List<LabelScore> = emptyList(),
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

    private val _internalState = MutableStateFlow(InternalState())

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
            precisionScore = internal.precisionScore,
            top3Labels = internal.top3Labels,
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
                    it.copy(
                        currentLabel = "—",
                        message = null,
                        aiHeardTranscription = null,
                        isAnalyzing = false,
                        precisionScore = 0f,
                        top3Labels = emptyList()
                    )
                }
                audioUseCase.start(viewModelScope, bytes) { snap ->
                    val safeTopScore = snap.topScore.safeUnitFloat()
                    val safeTop3 = snap.ranked
                        .take(3)
                        .map { it.copy(score = it.score.safeUnitFloat()) }
                    _internalState.update { s ->
                        s.copy(
                            currentLabel = snap.topLabel,
                            precisionScore = safeTopScore,
                            top3Labels = safeTop3
                        )
                    }
                    
                    if (isProcessingGrowl) return@start
                    
                    viewModelScope.launch {
                        val currentStats = repository.getPlayerStats().first()
                        
                        // We use the new 0.90 threshold for analyzing state UI feedback
                        val isLikelyGrowl = snap.topLabel.startsWith("Growl_") && safeTopScore >= 0.45f
                        if (isLikelyGrowl) {
                            _internalState.update { it.copy(isAnalyzing = true) }
                            isProcessingGrowl = true
                        } else {
                            // If it's not a growl (e.g. background or clean), don't trigger validator logic yet
                            // unless it's a very high confidence clean vocal to show anti-cheat feedback.
                            if (snap.topLabel == "Clean_Vocals" && safeTopScore >= 0.80f) {
                                isProcessingGrowl = true
                            } else {
                                return@launch
                            }
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
        val isRecording: Boolean = false,
        val currentLabel: String = "—",
        val message: String? = null,
        val showExitConfirmation: Boolean = false,
        val aiHeardTranscription: String? = null,
        val isAnalyzing: Boolean = false,
        val precisionScore: Float = 0f,
        val top3Labels: List<LabelScore> = emptyList(),
    )
}

private fun Float.safeUnitFloat(): Float {
    if (!isFinite()) return 0f
    return coerceIn(0f, 1f)
}
