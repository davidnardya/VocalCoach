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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.davidnardya.vocalcoach.domain.models.LabelScore
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.domain.models.resolveStageProgression
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository
import org.davidnardya.vocalcoach.domain.usecase.audio.AudioClassificationUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.HandleGrowlDetectionUseCase
import org.davidnardya.vocalcoach.domain.usecase.audio.LevelTrackerUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.CalculatePointsUseCase
import org.davidnardya.vocalcoach.domain.usecase.game.LevelUpCelebration
import org.davidnardya.vocalcoach.domain.usecase.game.PenaltyResult
import org.davidnardya.vocalcoach.domain.usecase.game.StreakManagementUseCase
import kotlin.math.floor

data class AudioClassificationUiState(
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
    val stageFeedback: String? = null,
    val stageCombo: Int = 0,
    /** Active level-up overlay; cleared after celebration duration. */
    val levelUpCelebration: LevelUpCelebration? = null,
    /** True when [levelUpCelebration] includes a streak milestone (+20 at 25, 50, …). */
    val isMilestoneLevelUp: Boolean = false,
)

class AudioClassificationViewModel(
    private val stage: TrainingStage,
    private val audioUseCase: AudioClassificationUseCase,
    private val repository: ScoreRepository,
    levelTrackerUseCase: LevelTrackerUseCase,
    private val handleGrowlDetectionUseCase: HandleGrowlDetectionUseCase,
    private val calculatePointsUseCase: CalculatePointsUseCase,
    private val streakManagementUseCase: StreakManagementUseCase,
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
                    delay(2500) // Slightly longer to see the cool animation
                    _levelUpCelebration.value = null
                }
            }
        }
    }

    /** Current score multiplier based on current combo and stage status. */
    val multiplier: StateFlow<Double> = combine(
        repository.getPlayerStats(),
        _internalState.map { it.stageCombo }
    ) { stats, combo ->
        val progression = resolveStageProgression(stats.level, null)
        val isPrevious = stage.ordinal < progression.maxUnlockedStage.ordinal
        calculatePointsUseCase.getCurrentMultiplier(combo, isPrevious)
    }.stateIn(
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
        val milestone = levelUpCelebration?.milestoneBonus
        val progression = resolveStageProgression(
            level = stats.level,
            preferredStage = stage,
        )
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
            precisionScore = internal.precisionScore,
            top3Labels = internal.top3Labels,
            currentStage = progression.currentStage,
            maxUnlockedStage = progression.maxUnlockedStage,
            stagePrompt = internal.stagePrompt,
            stageFeedback = internal.stageFeedback,
            stageCombo = internal.stageCombo,
            levelUpCelebration = levelUpCelebration,
            isMilestoneLevelUp = milestone != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AudioClassificationUiState(),
    )

    private var cachedModelBytes: ByteArray? = null
    private var lastScoringMs: Long = 0L
    private val scoringCooldownMs = 1200L

    fun onStartStopClicked(
        permissionGranted: Boolean,
        modelLoader: suspend () -> ByteArray,
    ) {
        if (!permissionGranted) {
            _internalState.update { it.copy(message = "Microphone permission is required.") }
            return
        }
        if (_internalState.value.isRecording) {
            stopRecordingAndTruncate()
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
                        maybeScoreAttempt(snap.topLabel, safeScore)
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

    private fun stopRecordingAndTruncate() {
        audioUseCase.stop()
        _internalState.update { it.copy(isRecording = false, message = null) }
        viewModelScope.launch {
            repository.updateStats { stats ->
                stats.copy(totalScore = floor(stats.totalScore))
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

    fun onConfirmExit() {
        stopRecordingAndTruncate()
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
        val precisionScore: Float = 0f,
        val top3Labels: List<LabelScore> = emptyList(),
        val stagePrompt: String = "Make any growl",
        val stageFeedback: String? = null,
        val stageCombo: Int = 0,
    )

    private suspend fun maybeScoreAttempt(label: String, score: Float) {
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
            repository.updateStats { stats ->
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val todayStr = today.toString()
                val rolled = org.davidnardya.vocalcoach.domain.models.PlayerStats.withDayRollover(stats, today)
                
                val progression = resolveStageProgression(rolled.level, null)
                val isPrevious = stage.ordinal < progression.maxUnlockedStage.ordinal
                val points = calculatePointsUseCase.pointsForGrowl(
                    comboBeforeHit = internal.stageCombo,
                    isPreviousStage = isPrevious
                )
                
                val total = rolled.totalScore + points
                val oldLevel = rolled.level
                val newLevel = org.davidnardya.vocalcoach.domain.models.PlayerStats.levelFromTotalScore(total)
                
                if (newLevel > oldLevel) {
                    val r = streakManagementUseCase.processLevelUpBranch(
                        rolled = rolled,
                        oldLevel = oldLevel,
                        newLevel = newLevel,
                        todayStr = todayStr,
                        totalScoreAfterGrowl = total
                    )
                    celebration = LevelUpCelebration(newLevel = newLevel, milestoneBonus = r.milestoneBonus)
                    rolled.copy(
                        totalScore = r.totalScore,
                        streakCount = r.streakCount,
                        lastLevelUpDate = r.lastLevelUpDate,
                        lastStreakIncrementDate = r.lastStreakIncrementDate,
                        levelsGainedToday = r.levelsGainedToday,
                        levelsGainedForDate = r.levelsGainedForDate,
                    )
                } else {
                    rolled.copy(totalScore = total)
                }
            }
            celebration?.let { c ->
                streakManagementUseCase.emitLevelUpCelebration(c)
                c.milestoneBonus?.let { bonus ->
                    streakManagementUseCase.emitMilestoneBonusForPresentation(bonus)
                }
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
        TrainingStage.Stage3 -> evaluatePhraseStage(
            combo = combo,
            growlDetected = growlDetected,
            vowel = vowel,
            targets = listOf("RAGE" to 'A', "VOID" to 'O', "STEEL" to 'E', "RIFT" to 'I', "BRUTE" to 'U'),
            label = "word",
        )
        TrainingStage.Stage4 -> evaluatePhraseStage(
            combo = combo,
            growlDetected = growlDetected,
            vowel = vowel,
            targets = listOf(
                "BURN THE NIGHT" to 'A',
                "HOLD THE STORM" to 'O',
                "BREAK THE SPELL" to 'E',
                "RIDE THE RIFT" to 'I',
                "CRUSH THE TRUTH" to 'U',
            ),
            label = "sentence",
        )
        TrainingStage.Stage5 -> evaluatePhraseStage(
            combo = combo,
            growlDetected = growlDetected,
            vowel = vowel,
            targets = listOf(
                "WE RISE / WE ROAR" to 'A',
                "COLD BLOOD / COLD IRON" to 'O',
                "STEEL NERVES / STEEL VEINS" to 'E',
                "SPIRIT / FURY / SPIRIT" to 'I',
                "TRUTH / BRUTE / TRUTH" to 'U',
            ),
            label = "verse",
        )
    }
}

private fun evaluatePhraseStage(
    combo: Int,
    growlDetected: Boolean,
    vowel: Char?,
    targets: List<Pair<String, Char>>,
    label: String,
): StageEvalResult {
    val current = targets[combo % targets.size]
    val success = growlDetected && vowel == current.second
    val nextCombo = if (success) combo + 1 else 0
    val nextTarget = targets[nextCombo % targets.size]
    return StageEvalResult(
        shouldCountAttempt = growlDetected,
        success = success,
        nextPrompt = "Target $label: ${nextTarget.first}",
        feedback = if (success) "Matched target!" else "Missed target $label",
    )
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
    TrainingStage.Stage3 -> "Target word: RAGE"
    TrainingStage.Stage4 -> "Target sentence: BURN THE NIGHT"
    TrainingStage.Stage5 -> "Target verse: WE RISE / WE ROAR"
}

private fun Float.safeUnitFloat(): Float {
    if (!isFinite()) return 0f
    return coerceIn(0f, 1f)
}
