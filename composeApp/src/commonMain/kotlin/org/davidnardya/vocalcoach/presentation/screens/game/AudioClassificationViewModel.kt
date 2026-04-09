package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.davidnardya.vocalcoach.data.gemini.AiVocalValidator
import org.davidnardya.vocalcoach.domain.developer.DeveloperDebugSettings
import org.davidnardya.vocalcoach.domain.models.LabelScore
import org.davidnardya.vocalcoach.domain.models.PlayerStats
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.domain.models.resolveStageProgression
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.audio.AudioClassificationUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.LevelTrackerUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.GetNextTargetUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.LevelUpCelebration
import org.davidnardya.vocalcoach.domain.usecase.game.MilestoneBonus
import org.davidnardya.vocalcoach.domain.usecase.game.PenaltyResult
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.UpdateStreakUseCase
import kotlin.math.floor

private const val RECORDING_ARM_DELAY_MS = 400L

/** How the current route validates attempts: Teachable Machine stream vs file + Gemini. */
enum class ValidationMode {
    REAL_TIME,
    RECORDING,
}

/** Lifecycle for recording-mode capture and Gemini analysis. */
enum class ProcessingState {
    IDLE,
    /** 500ms arm delay before [AudioRecorder.startRecording]; mic not yet capturing. */
    PREPARING,
    RECORDING,
    ANALYZING,
    SUCCESS,
    ERROR,
}

data class AudioClassificationUiState(
    val validationMode: ValidationMode = ValidationMode.REAL_TIME,
    val processingState: ProcessingState = ProcessingState.IDLE,
    val isRecording: Boolean = false,
    val currentLabel: String = "—",
    val totalScore: Double = 0.0,
    val currentLevel: Int = 1,
    val levelsGainedToday: Int = 0,
    val streakCount: Int = 0,
    val scoreMultiplier: Double = 1.0,
    val message: String? = null,
    val showExitConfirmation: Boolean = false,
    val precisionScore: Float = 0f,
    val top3Labels: List<LabelScore> = emptyList(),
    val currentStage: TrainingStage = TrainingStage.Stage1,
    val maxUnlockedStage: TrainingStage = TrainingStage.Stage1,
    val stagePrompt: String = "Make any growl",
    /** Current plain-text target for recording stages; empty in real-time stages. */
    val currentTarget: String = "",
    val stageFeedback: String? = null,
    val stageCombo: Int = 0,
    /** Session hot streak (consecutive successes); same source as [stageCombo]. */
    val currentHotStreak: Int = 0,
    /** Last Gemini accuracy (0–100) after a recording-mode analysis. */
    val lastAccuracyScore: Double? = null,
    /** Last detected extreme-vocal technique from Gemini analysis. */
    val lastTechniqueDetected: String? = null,
    /** Full Gemini feedback text from the latest recording analysis. */
    val lastFullFeedback: String? = null,
    /** Controls "Vocal Analysis" dialog visibility. */
    val showAnalysisDialog: Boolean = false,
    /** Active level-up overlay; cleared after celebration duration. */
    val levelUpCelebration: LevelUpCelebration? = null,
    /** True when [levelUpCelebration] includes a streak milestone (+20 at 25, 50, …). */
    val isMilestoneLevelUp: Boolean = false,
    /** True only while the file recorder is actively capturing (after arm delay). */
    val micLive: Boolean = false,
)

/**
 * Game session ViewModel: Stages 1–2 use Teachable Machine in real time; Stages 3–5 use file capture + [AiVocalValidator].
 */
class AudioClassificationViewModel(
    private val stage: TrainingStage,
    private val audioUseCase: AudioClassificationUseCase,
    private val repository: ScoreRepository,
    levelTrackerUseCase: LevelTrackerUseCase,
    private val calculatePointsUseCase: CalculatePointsUseCase,
    private val streakManagementUseCase: StreakManagementUseCase,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val getNextTargetUseCase: GetNextTargetUseCase,
    private val fileAudioRecorder: AudioRecorder,
    private val aiVocalValidator: AiVocalValidator?,
) : ViewModel() {

    private val validationMode: ValidationMode =
        if (stage.ordinal >= TrainingStage.Stage3.ordinal) {
            ValidationMode.RECORDING
        } else {
            ValidationMode.REAL_TIME
        }

    private val _internalState = MutableStateFlow(
        InternalState(isRecording = false, currentLabel = "—"),
    )

    private val _levelUpCelebration = MutableStateFlow<LevelUpCelebration?>(null)
    private var clearLevelUpOverlayJob: Job? = null
    private var resetProcessingJob: Job? = null

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
                    delay(2500)
                    _levelUpCelebration.value = null
                }
            }
        }

        if (validationMode == ValidationMode.RECORDING) {
            viewModelScope.launch {
                prepareNextRecordingTarget()
            }
        }
    }

    /** Points awarded for the next successful hit (tiered by stage role and session streak). */
    val multiplier: StateFlow<Double> = combine(
        repository.getPlayerStats(),
        _internalState.map { it.stageCombo },
        DeveloperDebugSettings.maxUnlockedStageBoost,
    ) { stats, sessionStreakBeforeNextHit, devBoost ->
        val progression = resolveStageProgression(stats.level, null, devBoost)
        val activeStage = stage.ordinal + 1
        val maxUnlockedStage = progression.maxUnlockedStage.ordinal + 1
        calculatePointsUseCase.previewPointsForNextHit(
            activeStage = activeStage,
            maxUnlockedStage = maxUnlockedStage,
            sessionStreakBeforeHit = sessionStreakBeforeNextHit,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0,
    )

    val state: StateFlow<AudioClassificationUiState> = combine(
        combine(
            _internalState,
            repository.getPlayerStats(),
            _levelUpCelebration,
            multiplier,
            fileAudioRecorder.isRecording,
        ) { internal, stats, levelUpCelebration, mult, fileIsRecording ->
            arrayOf<Any?>(internal, stats, levelUpCelebration, mult, fileIsRecording)
        },
        DeveloperDebugSettings.maxUnlockedStageBoost,
    ) { pack, devBoost ->
        val internal = pack[0] as InternalState
        val stats = pack[1] as PlayerStats
        val levelUpCelebration = pack[2] as LevelUpCelebration?
        val mult = pack[3] as Double
        val fileIsRecording = pack[4] as Boolean
        val milestone = levelUpCelebration?.milestoneBonus
        val progression = resolveStageProgression(
            level = stats.level,
            preferredStage = stage,
            devMaxUnlockedBoost = devBoost,
        )
        val isSessionActive = when (validationMode) {
            ValidationMode.REAL_TIME -> internal.isRecording
            ValidationMode.RECORDING ->
                fileIsRecording ||
                    internal.processingState == ProcessingState.RECORDING ||
                    internal.processingState == ProcessingState.PREPARING
        }
        val currentTarget = if (validationMode == ValidationMode.RECORDING) internal.currentTarget else ""
        AudioClassificationUiState(
            validationMode = validationMode,
            processingState = internal.processingState,
            isRecording = isSessionActive,
            currentLabel = internal.currentLabel,
            totalScore = stats.totalScore,
            currentLevel = stats.level,
            levelsGainedToday = stats.levelsGainedToday,
            streakCount = stats.currentStreak,
            scoreMultiplier = mult,
            message = internal.message,
            showExitConfirmation = internal.showExitConfirmation,
            precisionScore = internal.precisionScore,
            top3Labels = internal.top3Labels,
            currentStage = progression.currentStage,
            maxUnlockedStage = progression.maxUnlockedStage,
            stagePrompt = internal.stagePrompt,
            currentTarget = currentTarget,
            stageFeedback = internal.stageFeedback,
            stageCombo = internal.stageCombo,
            currentHotStreak = internal.stageCombo,
            lastAccuracyScore = internal.lastAccuracyScore,
            lastTechniqueDetected = internal.lastTechniqueDetected,
            lastFullFeedback = internal.lastFullFeedback,
            showAnalysisDialog = internal.showAnalysisDialog,
            levelUpCelebration = levelUpCelebration,
            isMilestoneLevelUp = milestone != null,
            micLive = fileIsRecording,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AudioClassificationUiState(validationMode = validationMode),
    )

    private var cachedModelBytes: ByteArray? = null
    private var lastScoringMs: Long = 0L
    private val scoringCooldownMs = 1200L

    private var recordingPrepJob: Job? = null

    private suspend fun prepareNextRecordingTarget() {
        if (validationMode != ValidationMode.RECORDING) return
        val nextTarget = getNextTargetUseCase(stage)
        if (nextTarget.isBlank()) return
        _internalState.update {
            it.copy(
                currentTarget = nextTarget,
                stagePrompt = recordingStagePrompt(stage, nextTarget),
            )
        }
    }

    private fun cancelRecordingPrep() {
        recordingPrepJob?.cancel()
        recordingPrepJob = null
        if (_internalState.value.processingState == ProcessingState.PREPARING) {
            _internalState.update { it.copy(processingState = ProcessingState.IDLE) }
        }
    }

    /**
     * Recording-only: shows [ProcessingState.PREPARING], waits [RECORDING_ARM_DELAY_MS], then calls
     * [AudioRecorder.startRecording]. Used by tap and hold start paths.
     */
    private fun onRecordStarted() {
        cancelRecordingPrep()
        resetProcessingJob?.cancel()
        recordingPrepJob = viewModelScope.launch {
            _internalState.update {
                it.copy(
                    processingState = ProcessingState.PREPARING,
                    message = null,
                    stageFeedback = null,
                    lastAccuracyScore = null,
                )
            }
            delay(RECORDING_ARM_DELAY_MS)
            if (!isActive) return@launch
            if (fileAudioRecorder.isRecording.value) return@launch
            fileAudioRecorder.startRecording()
            _internalState.update { it.copy(processingState = ProcessingState.RECORDING) }
        }
    }

    /**
     * Primary control: Teachable Machine (stages 1–2) or record / stop + analyze (stages 3–5).
     */
    fun onStartStopClicked(
        permissionGranted: Boolean,
        modelLoader: suspend () -> ByteArray,
    ) {
        when (validationMode) {
            ValidationMode.RECORDING -> onRecordToggle(permissionGranted)
            ValidationMode.REAL_TIME -> onRealTimeStartStop(permissionGranted, modelLoader)
        }
    }

    /**
     * Recording-mode only: start/stop capture; on stop, runs [AiVocalValidator.analyzeVocal] on the file path.
     */
    fun onRecordToggle(permissionGranted: Boolean) {
        if (validationMode != ValidationMode.RECORDING) return
        handleRecordingToggle(permissionGranted)
    }

    /**
     * Press-and-hold recording: [pressed] true = start capture, false = stop and analyze.
     * Ignored when not in [ValidationMode.RECORDING].
     */
    fun onRecordingPressChanged(pressed: Boolean, permissionGranted: Boolean) {
        if (validationMode != ValidationMode.RECORDING) return
        if (!permissionGranted) {
            if (pressed) {
                _internalState.update { it.copy(message = "Microphone permission is required.") }
            }
            return
        }
        val validator = aiVocalValidator
        if (validator == null) {
            if (pressed) {
                _internalState.update {
                    it.copy(
                        processingState = ProcessingState.ERROR,
                        message = "AI validation is not available for this stage.",
                    )
                }
            }
            return
        }
        if (pressed) {
            if (_internalState.value.processingState == ProcessingState.ANALYZING) return
            if (fileAudioRecorder.isRecording.value) return
            if (_internalState.value.processingState == ProcessingState.PREPARING) return
            resetProcessingJob?.cancel()
            onRecordStarted()
        } else {
            cancelRecordingPrep()
            if (fileAudioRecorder.isRecording.value) {
                viewModelScope.launch {
                    runRecordingAnalysis(validator)
                }
            }
        }
    }

    private fun onRealTimeStartStop(
        permissionGranted: Boolean,
        modelLoader: suspend () -> ByteArray,
    ) {
        if (!permissionGranted) {
            _internalState.update { it.copy(message = "Microphone permission is required.") }
            return
        }
        if (_internalState.value.isRecording) {
            stopRealTimeSession()
            return
        }
        viewModelScope.launch {
            try {
                val bytes = cachedModelBytes ?: modelLoader().also { cachedModelBytes = it }
                _internalState.update {
                    it.copy(
                        currentLabel = "—",
                        message = null,
                        precisionScore = 0f,
                        top3Labels = emptyList(),
                        stageFeedback = null,
                        stagePrompt = stagePromptFor(stage, 0),
                        stageCombo = 0,
                        processingState = ProcessingState.IDLE,
                        lastAccuracyScore = null,
                    )
                }
                audioUseCase.start(viewModelScope, bytes) { snap ->
                    val safeScore = snap.topScore.safeUnitFloat()
                    val safeTop3 = snap.ranked.take(3).map { ls -> ls.copy(score = ls.score.safeUnitFloat()) }
                    _internalState.update { s ->
                        s.copy(
                            currentLabel = snap.topLabel,
                            precisionScore = safeScore,
                            top3Labels = safeTop3,
                        )
                    }
                    viewModelScope.launch {
                        maybeScoreRealTimeAttempt(snap.topLabel, safeScore)
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

    private fun handleRecordingToggle(permissionGranted: Boolean) {
        if (!permissionGranted) {
            _internalState.update { it.copy(message = "Microphone permission is required.") }
            return
        }
        val validator = aiVocalValidator
        if (validator == null) {
            _internalState.update {
                it.copy(
                    processingState = ProcessingState.ERROR,
                    message = "AI validation is not available for this stage.",
                )
            }
            return
        }
        when {
            fileAudioRecorder.isRecording.value -> {
                viewModelScope.launch {
                    runRecordingAnalysis(validator)
                }
            }
            _internalState.value.processingState == ProcessingState.ANALYZING -> return
            _internalState.value.processingState == ProcessingState.PREPARING -> {
                cancelRecordingPrep()
            }
            else -> {
                resetProcessingJob?.cancel()
                onRecordStarted()
            }
        }
    }

    private suspend fun runRecordingAnalysis(validator: AiVocalValidator) {
        _internalState.update { it.copy(processingState = ProcessingState.ANALYZING) }
        val path = fileAudioRecorder.stopRecording()
        if (path == null) {
            _internalState.update {
                it.copy(
                    processingState = ProcessingState.ERROR,
                    message = "No audio captured.",
                    stageFeedback = null,
                )
            }
            return
        }
        val internal = _internalState.value
        val targetPhrase = internal.currentTarget.ifBlank {
            getNextTargetUseCase(stage)
        }
        val result = validator.analyzeVocal(path, mimeType = "audio/wav", currentTarget = targetPhrase)
        result.fold(
            onSuccess = { analysis ->
                val techPercent = (analysis.technicalScore * 100).toInt().coerceIn(0, 100)
                val feedbackText =
                    if (analysis.techniqueDetected.isNotBlank()) {
                        "${analysis.feedback} (technique: ${analysis.techniqueDetected})"
                    } else {
                        "${analysis.feedback} (growl technique $techPercent%)"
                    }
                val nextCombo = if (analysis.isSuccessful) internal.stageCombo + 1 else 0
                if (analysis.isSuccessful) {
                    awardPointsForSuccessfulRecordingHit(internal.stageCombo)
                }
                _internalState.update {
                    it.copy(
                        stageCombo = nextCombo,
                        stageFeedback = feedbackText,
                        lastAccuracyScore = analysis.technicalScore * 100.0,
                        lastTechniqueDetected = analysis.techniqueDetected.takeIf { t -> t.isNotBlank() },
                        lastFullFeedback = analysis.feedback,
                        processingState = if (analysis.isSuccessful) ProcessingState.SUCCESS else ProcessingState.IDLE,
                        message = null,
                    )
                }
                if (analysis.isSuccessful) {
                    resetProcessingJob?.cancel()
                    resetProcessingJob = viewModelScope.launch {
                        delay(1600)
                        _internalState.update { s ->
                            if (s.processingState == ProcessingState.SUCCESS) {
                                s.copy(processingState = ProcessingState.IDLE)
                            } else {
                                s
                            }
                        }
                    }
                }
                viewModelScope.launch {
                    prepareNextRecordingTarget()
                }
            },
            onFailure = { e ->
                _internalState.update {
                    it.copy(
                        processingState = ProcessingState.ERROR,
                        stageFeedback = null,
                        lastAccuracyScore = null,
                        lastTechniqueDetected = null,
                        lastFullFeedback = null,
                        message = e.message?.takeIf { m -> m.isNotBlank() }
                            ?: "Could not analyze audio. Check your connection and try again.",
                    )
                }
            },
        )
    }

    fun onViewAnalysisClicked() {
        val state = _internalState.value
        if (validationMode != ValidationMode.RECORDING) return
        if (state.lastAccuracyScore == null) return
        _internalState.update { it.copy(showAnalysisDialog = true) }
    }

    fun onDismissAnalysisDialog() {
        _internalState.update { it.copy(showAnalysisDialog = false) }
    }

    private suspend fun awardPointsForSuccessfulRecordingHit(sessionStreakBeforeHit: Int) {
        var celebration: LevelUpCelebration? = null
        var streakMilestone: MilestoneBonus? = null
        repository.updateStats { stats ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val todayStr = today.toString()
            val rolled = org.davidnardya.vocalcoach.domain.models.PlayerStats.withDayRollover(stats, today)

            val progression = resolveStageProgression(
                rolled.level,
                null,
                DeveloperDebugSettings.maxUnlockedStageBoost.value,
            )
            val activeStage = stage.ordinal + 1
            val maxUnlockedStage = progression.maxUnlockedStage.ordinal + 1
            val hotStreakIncludingThisHit = sessionStreakBeforeHit + 1
            val points = calculatePointsUseCase.pointsForHit(
                activeStage = activeStage,
                maxUnlockedStage = maxUnlockedStage,
                hotStreakCount = hotStreakIncludingThisHit,
            )

            val total = rolled.totalScore + points
            val oldLevel = rolled.level
            val newLevel = org.davidnardya.vocalcoach.domain.models.PlayerStats.levelFromTotalScore(total)

            val next = if (newLevel > oldLevel) {
                val r = streakManagementUseCase.processLevelUpBranch(
                    rolled = rolled,
                    oldLevel = oldLevel,
                    newLevel = newLevel,
                    todayStr = todayStr,
                    totalScoreAfterGrowl = total,
                )
                celebration = LevelUpCelebration(newLevel = newLevel, milestoneBonus = r.milestoneBonus)
                rolled.copy(
                    totalScore = r.totalScore,
                    currentStreak = r.currentStreak,
                    lastLevelUpDate = r.lastLevelUpDate,
                    lastStreakIncrementDate = r.lastStreakIncrementDate,
                    levelsGainedToday = r.levelsGainedToday,
                    levelsGainedForDate = r.levelsGainedForDate,
                )
            } else {
                rolled.copy(totalScore = total)
            }

            val streakResult = updateStreakUseCase.applySessionCompletion(next)
            streakMilestone = streakResult.milestoneBonus
            streakResult.stats
        }
        celebration?.let { c ->
            streakManagementUseCase.emitLevelUpCelebration(c)
            c.milestoneBonus?.let { bonus ->
                streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
            }
        }
        streakMilestone?.let { bonus ->
            streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
        }
    }

    private fun stopRealTimeSession() {
        audioUseCase.stop()
        _internalState.update { it.copy(isRecording = false, message = null) }
        viewModelScope.launch {
            repository.updateStats { stats ->
                stats.copy(totalScore = floor(stats.totalScore))
            }
        }
    }

    fun onBackRequested() {
        if (sessionBusy()) {
            _internalState.update { it.copy(showExitConfirmation = true) }
        }
    }

    private fun sessionBusy(): Boolean = when (validationMode) {
        ValidationMode.REAL_TIME -> _internalState.value.isRecording
        ValidationMode.RECORDING ->
            fileAudioRecorder.isRecording.value ||
                _internalState.value.processingState == ProcessingState.ANALYZING ||
                _internalState.value.processingState == ProcessingState.RECORDING ||
                _internalState.value.processingState == ProcessingState.PREPARING
    }

    fun onDismissExitConfirmation() {
        _internalState.update { it.copy(showExitConfirmation = false) }
    }

    fun onConfirmExit() {
        stopActiveSession()
    }

    private fun stopActiveSession() {
        when (validationMode) {
            ValidationMode.REAL_TIME -> stopRealTimeSession()
            ValidationMode.RECORDING -> {
                resetProcessingJob?.cancel()
                cancelRecordingPrep()
                if (fileAudioRecorder.isRecording.value) {
                    fileAudioRecorder.stopRecording()
                }
                _internalState.update {
                    it.copy(
                        processingState = ProcessingState.IDLE,
                        message = null,
                        showExitConfirmation = false,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        recordingPrepJob?.cancel()
        audioUseCase.stop()
        if (fileAudioRecorder.isRecording.value) {
            fileAudioRecorder.stopRecording()
        }
        super.onCleared()
    }

    private data class InternalState(
        val isRecording: Boolean,
        val currentLabel: String,
        val currentTarget: String = "",
        val processingState: ProcessingState = ProcessingState.IDLE,
        val message: String? = null,
        val showExitConfirmation: Boolean = false,
        val precisionScore: Float = 0f,
        val top3Labels: List<LabelScore> = emptyList(),
        val stagePrompt: String = "Make any growl",
        val stageFeedback: String? = null,
        val stageCombo: Int = 0,
        val lastAccuracyScore: Double? = null,
        val lastTechniqueDetected: String? = null,
        val lastFullFeedback: String? = null,
        val showAnalysisDialog: Boolean = false,
    )

    private suspend fun maybeScoreRealTimeAttempt(label: String, score: Float) {
        if (validationMode != ValidationMode.REAL_TIME) return
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastScoringMs < scoringCooldownMs) return
        lastScoringMs = now

        val internal = _internalState.value
        val result = evaluateStage(
            stage = stage,
            label = label,
            score = score,
            combo = internal.stageCombo,
        )
        if (!result.shouldCountAttempt) return

        if (result.success) {
            var celebration: LevelUpCelebration? = null
            var streakMilestone: MilestoneBonus? = null
            repository.updateStats { stats ->
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val todayStr = today.toString()
                val rolled = org.davidnardya.vocalcoach.domain.models.PlayerStats.withDayRollover(stats, today)

                val progression = resolveStageProgression(
                    rolled.level,
                    null,
                    DeveloperDebugSettings.maxUnlockedStageBoost.value,
                )
                val activeStage = stage.ordinal + 1
                val maxUnlockedStage = progression.maxUnlockedStage.ordinal + 1
                val hotStreakIncludingThisHit = internal.stageCombo + 1
                val points = calculatePointsUseCase.pointsForHit(
                    activeStage = activeStage,
                    maxUnlockedStage = maxUnlockedStage,
                    hotStreakCount = hotStreakIncludingThisHit,
                )

                val total = rolled.totalScore + points
                val oldLevel = rolled.level
                val newLevel = org.davidnardya.vocalcoach.domain.models.PlayerStats.levelFromTotalScore(total)

                val next = if (newLevel > oldLevel) {
                    val r = streakManagementUseCase.processLevelUpBranch(
                        rolled = rolled,
                        oldLevel = oldLevel,
                        newLevel = newLevel,
                        todayStr = todayStr,
                        totalScoreAfterGrowl = total,
                    )
                    celebration = LevelUpCelebration(newLevel = newLevel, milestoneBonus = r.milestoneBonus)
                    rolled.copy(
                        totalScore = r.totalScore,
                        currentStreak = r.currentStreak,
                        lastLevelUpDate = r.lastLevelUpDate,
                        lastStreakIncrementDate = r.lastStreakIncrementDate,
                        levelsGainedToday = r.levelsGainedToday,
                        levelsGainedForDate = r.levelsGainedForDate,
                    )
                } else {
                    rolled.copy(totalScore = total)
                }

                val streakResult = updateStreakUseCase.applySessionCompletion(next)
                streakMilestone = streakResult.milestoneBonus
                streakResult.stats
            }
            celebration?.let { c ->
                streakManagementUseCase.emitLevelUpCelebration(c)
                c.milestoneBonus?.let { bonus ->
                    streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
                }
            }
            streakMilestone?.let { bonus ->
                streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
            }
        }

        _internalState.update {
            it.copy(
                stageCombo = if (result.success) internal.stageCombo + 1 else 0,
                stagePrompt = result.nextPrompt,
                stageFeedback = result.feedback,
            )
        }
    }
}

/** Alias for documentation and navigation code that refers to the arena session as "Game". */
typealias GameViewModel = AudioClassificationViewModel

private data class StageEvalResult(
    val shouldCountAttempt: Boolean,
    val success: Boolean,
    val nextPrompt: String,
    val feedback: String,
)

private fun evaluateStage(
    stage: TrainingStage,
    label: String,
    score: Float,
    combo: Int,
): StageEvalResult {
    val parsed = parseModelLabel(label)
    val vowel = parsed.vowel
    val growlDetected = parsed.type == ModelLabelType.Growl && score >= 0.45f
    return when (stage) {
        TrainingStage.Stage1 -> {
            StageEvalResult(
                shouldCountAttempt = parsed.type != ModelLabelType.Background,
                success = growlDetected,
                nextPrompt = stagePromptFor(stage, 0),
                feedback = if (growlDetected) "Success!" else "No growl detected",
            )
        }
        TrainingStage.Stage2 -> {
            val target = nextTargetVowel(combo)
            val success = growlDetected && vowel == target
            val nextCombo = if (success) combo + 1 else 0
            StageEvalResult(
                shouldCountAttempt = growlDetected,
                success = success,
                nextPrompt = "Target vowel: ${nextTargetVowel(nextCombo).uppercaseChar()}",
                feedback = if (success) "Correct vowel!" else "Missed target vowel",
            )
        }
        // Stages 3–5 use recording + Gemini flow, not real-time label matching.
        TrainingStage.Stage3,
        TrainingStage.Stage4,
        TrainingStage.Stage5,
        -> StageEvalResult(
            shouldCountAttempt = false,
            success = false,
            nextPrompt = stagePromptFor(stage, combo),
            feedback = "",
        )
    }
}

private fun recordingStagePrompt(stage: TrainingStage, target: String): String {
    if (target.isBlank()) return stagePromptFor(stage, 0)
    val label = when (stage) {
        TrainingStage.Stage3 -> "word"
        TrainingStage.Stage4 -> "sentence"
        TrainingStage.Stage5 -> "verse"
        else -> "phrase"
    }
    return "Target $label: $target"
}


private enum class ModelLabelType {
    Background,
    Clean,
    Growl,
}

private data class ParsedModelLabel(
    val type: ModelLabelType,
    val vowel: Char?,
)

private fun parseModelLabel(label: String): ParsedModelLabel {
    val normalized = label.trim().lowercase()
    if (normalized.contains("background") || normalized == "bg_noise") {
        return ParsedModelLabel(ModelLabelType.Background, null)
    }
    if (normalized.contains("clean")) {
        return ParsedModelLabel(ModelLabelType.Clean, null)
    }

    val isGrowl = normalized.contains("growl")
    if (!isGrowl) return ParsedModelLabel(ModelLabelType.Background, null)

    val vowel = when {
        normalized.endsWith("_a") || normalized.endsWith(" a") || normalized.endsWith("a") -> 'A'
        normalized.endsWith("_o") || normalized.endsWith(" o") || normalized.endsWith("o") -> 'O'
        normalized.endsWith("_e") || normalized.endsWith(" e") || normalized.endsWith("e") -> 'E'
        normalized.endsWith("_i") || normalized.endsWith(" i") || normalized.endsWith("i") -> 'I'
        normalized.endsWith("_u") || normalized.endsWith(" u") || normalized.endsWith("u") -> 'U'
        else -> null
    }
    return ParsedModelLabel(ModelLabelType.Growl, vowel)
}

private fun nextTargetVowel(combo: Int): Char {
    val seq = listOf('A', 'O', 'E', 'I', 'U')
    return seq[combo % seq.size]
}

private fun stagePromptFor(stage: TrainingStage, combo: Int): String = when (stage) {
    TrainingStage.Stage1 -> "Make any growl"
    TrainingStage.Stage2 -> "Target vowel: ${nextTargetVowel(combo).uppercaseChar()}"
    TrainingStage.Stage3 -> "Target word: Ready"
    TrainingStage.Stage4 -> "Target sentence: Ready"
    TrainingStage.Stage5 -> "Target verse: Ready"
}

private fun Float.safeUnitFloat(): Float {
    if (!isFinite()) return 0f
    return coerceIn(0f, 1f)
}
