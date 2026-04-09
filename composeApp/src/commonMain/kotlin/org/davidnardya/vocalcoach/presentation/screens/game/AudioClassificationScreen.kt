package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.core.util.PlatformBackHandler
import org.davidnardya.vocalcoach.core.util.playRecordingArmCue
import org.davidnardya.vocalcoach.core.util.rememberMicrophonePermissionState
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vocalcoach.composeapp.generated.resources.Res
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

private const val MODEL_RESOURCE_PATH = "files/soundclassifier_with_metadata.tflite"

/** One decimal when needed (e.g. 0.5); whole scores without a trailing ".0". */
private fun formatScoreForDisplay(value: Double): String {
    val r = round(value * 10.0) / 10.0
    val whole = floor(r).toLong()
    val frac = round((r - whole) * 10.0).toInt()
    return if (frac == 0) whole.toString() else "$whole.$frac"
}

/** UI-only: separates critique text from the appended growl-technique suffix from the ViewModel. */
private fun stripGrowlTechniqueSuffix(feedback: String): String =
    feedback
        .replace(Regex("""\s*\(growl technique \d+%\)\s*$"""), "")
        .replace(Regex("""\s*\(technique: [^)]+\)\s*$"""), "")
        .trim()

/**
 * Teaser: first two newline-separated lines, or a capped prefix when Gemini returns one long paragraph.
 */
private fun feedbackTeaserTwoLines(full: String, singleLineCap: Int = 140): String {
    val t = full.replace("\r\n", "\n").trim()
    if (t.isEmpty()) return ""
    val lines = t.lines()
    return when {
        lines.size >= 2 -> lines.take(2).joinToString("\n")
        lines.size == 1 && lines[0].length > singleLineCap ->
            lines[0].take(singleLineCap).trim() + "…"
        else -> t
    }
}

private fun teaserIsShorterThanFull(teaser: String, full: String): Boolean =
    full.isNotEmpty() && teaser != full.trim()

/** KMP-safe display for technical score in 0.00–1.00 form (matches Gemini `technical_score`). */
private fun formatTechnicalScore01Text(unit: Double): String {
    val bps = (unit.coerceIn(0.0, 1.0) * 100.0).roundToInt().coerceIn(0, 100)
    val whole = bps / 100
    val frac = bps % 100
    return "$whole.${frac.toString().padStart(2, '0')}"
}

private val ElectricOrange = Color(0xFFFF8F00)
private val IndustrialOrange = Color(0xFFFF8F00)
private val BloodRed = Color(0xFFB22222)
private val Gold = Color(0xFFFFD700)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AudioClassificationRoute(
    viewModel: AudioClassificationViewModel,
    onBack: () -> Unit,
) {
    val mic = rememberMicrophonePermissionState()
    val ui by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    val shake = remember { Animatable(0f) }

    val blockBack =
        ui.isRecording ||
            ui.processingState == ProcessingState.ANALYZING ||
            ui.processingState == ProcessingState.RECORDING ||
            ui.processingState == ProcessingState.PREPARING
    PlatformBackHandler(enabled = blockBack) {
        viewModel.onBackRequested()
    }

    LaunchedEffect(Unit) {
        viewModel.penaltyEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.milestoneBonusEvents.collectLatest {
            snackbarHostState.showSnackbar("LEGENDARY STREAK! +20 MILESTONE BONUS!")
        }
    }

    LaunchedEffect(ui.levelUpCelebration) {
        if (ui.levelUpCelebration != null) {
            launch {
                runScreenShake200Ms(shake)
            }
        }
    }

    // Trigger feedback exactly when mic becomes live (after pre-roll delay).
    LaunchedEffect(ui.micLive) {
        if (ui.micLive) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            playRecordingArmCue()
        }
    }

    if (ui.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissExitConfirmation() },
            title = { Text("Leave the arena?") },
            text = { Text("Going back will stop the current recording.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDismissExitConfirmation()
                        onBack()
                    },
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissExitConfirmation() }) {
                    Text("Keep playing")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameScreen(
            modifier = Modifier
                .graphicsLayer { translationX = shake.value },
            ui = ui,
            micGranted = mic.isGranted,
            snackbarHostState = snackbarHostState,
            onRequestMic = { mic.requestPermission() },
            onStartStop = {
                viewModel.onStartStopClicked(mic.isGranted) { Res.readBytes(MODEL_RESOURCE_PATH) }
            },
            onRecordingPressChanged = { pressed ->
                viewModel.onRecordingPressChanged(pressed, mic.isGranted)
            },
            onBack = {
                if (
                    ui.isRecording ||
                        ui.processingState == ProcessingState.ANALYZING ||
                        ui.processingState == ProcessingState.PREPARING
                ) {
                    viewModel.onBackRequested()
                } else {
                    onBack()
                }
            },
            onViewAnalysis = { viewModel.onViewAnalysisClicked() },
            onDismissAnalysisDialog = { viewModel.onDismissAnalysisDialog() },
        )

        val celebration = ui.levelUpCelebration
        if (celebration != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
            ) {
                LevelUpAnimation(
                    newLevel = celebration.newLevel,
                    modifier = Modifier.fillMaxSize(),
                    legendaryStreakMilestone = ui.isMilestoneLevelUp,
                )
            }
        }
    }
}

/**
 * Arena layout: live meters / target stay pinned; prompts and Gemini feedback scroll.
 * Audio capture and TFLite run in the ViewModel / platform layer — scrolling only affects Compose layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    ui: AudioClassificationUiState,
    micGranted: Boolean,
    snackbarHostState: SnackbarHostState,
    onRequestMic: () -> Unit,
    onStartStop: () -> Unit,
    onRecordingPressChanged: (pressed: Boolean) -> Unit,
    onBack: () -> Unit,
    onViewAnalysis: () -> Unit,
    onDismissAnalysisDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GROWL ARENA",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (ui.validationMode == ValidationMode.REAL_TIME) {
                GameScreenBottomBar(
                    ui = ui,
                    micGranted = micGranted,
                    onRequestMic = onRequestMic,
                    onStartStop = onStartStop,
                )
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (ui.showAnalysisDialog) {
                    VocalAnalysisDialog(
                        ui = ui,
                        onDismiss = onDismissAnalysisDialog,
                    )
                }
                when (ui.validationMode) {
                    ValidationMode.REAL_TIME -> {
                        GameScoreHeader(ui = ui)
                        PinnedRealTimeLiveSection(ui = ui)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            RealTimeScrollableSection(ui = ui)
                        }
                    }
                    ValidationMode.RECORDING -> {
                        RecordingStageLayout(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            ui = ui,
                            micGranted = micGranted,
                            onRequestMic = onRequestMic,
                            onStartStop = onStartStop,
                            onRecordingPressChanged = onRecordingPressChanged,
                            onViewAnalysis = onViewAnalysis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VocalAnalysisDialog(
    ui: AudioClassificationUiState,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121417),
            border = BorderStroke(1.dp, Color(0xFF5E6673)),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "VOCAL ANALYSIS",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White,
                )
                Text(
                    text = "View Analysis",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                StatLine(
                    label = "technical_score",
                    value = ui.lastAccuracyScore?.let { "${it.toInt()}%" } ?: "—",
                )
                StatLine(
                    label = "technique_detected",
                    value = ui.lastTechniqueDetected ?: "unknown",
                )
                if (ui.validationMode == ValidationMode.REAL_TIME && ui.top3Labels.isNotEmpty()) {
                    Text(
                        text = "Raw model probabilities",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    ui.top3Labels.forEach { ls ->
                        Text(
                            text = "· ${ls.label}: ${(ls.score.coerceIn(0f, 1f) * 100f).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    }
                }
                Text(
                    text = "feedback",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.9f),
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = ui.lastFullFeedback ?: ui.stageFeedback ?: "No feedback yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun GameScreenBottomBar(
    ui: AudioClassificationUiState,
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    onStartStop: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MicPrimaryButton(
                micGranted = micGranted,
                onRequestMic = onRequestMic,
                onStartStop = onStartStop,
                isRecording = ui.isRecording,
                analyzing = false,
                validationMode = ValidationMode.REAL_TIME,
            )
        }
    }
}

@Composable
private fun PinnedRealTimeLiveSection(ui: AudioClassificationUiState) {
    val isGrowl = ui.currentLabel.contains("Growl", ignoreCase = true)
    val precision by animateFloatAsState(
        targetValue = ui.precisionScore.safeUnitFloat(),
        animationSpec = tween(280, easing = LinearEasing),
        label = "precision",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "VOCAL METER",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RageMeterBar(progress = precision, modifier = Modifier.fillMaxWidth(0.92f))
        Text(
            text = when {
                isGrowl -> "GROWL!"
                ui.isRecording -> "Listening…"
                else -> "Ready"
            },
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = if (isGrowl) BloodRed else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RealTimeScrollableSection(ui: AudioClassificationUiState) {
    Text(
        text = ui.stagePrompt,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center,
    )
    ui.stageFeedback?.let { feedback ->
        Text(
            text = feedback,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
    }
    ui.message?.let { err ->
        Text(
            text = err,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
    TechnicalStatsExpandable(ui = ui)
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * Target + prep status stay fixed; only coach feedback / technical stats scroll so the target
 * is never pushed off-screen. Mic actions stay pinned below the feedback scroll area.
 */
@Composable
private fun RecordingStageLayout(
    ui: AudioClassificationUiState,
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    onStartStop: () -> Unit,
    onRecordingPressChanged: (Boolean) -> Unit,
    onViewAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAnalyzing = ui.processingState == ProcessingState.ANALYZING
    val targetText = ui.currentTarget.ifBlank { ui.stagePrompt }
    val feedbackScroll = rememberScrollState()

    LaunchedEffect(ui.stageFeedback) {
        val feedback = ui.stageFeedback
        if (feedback.isNullOrBlank()) return@LaunchedEffect
        repeat(2) { attempt ->
            delay(if (attempt == 0) 48 else 120)
            val max = feedbackScroll.maxValue
            if (max > 0) {
                feedbackScroll.scrollTo(max)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RecordingHeaderRow(
            stageTitle = ui.currentStage.title,
            canViewAnalysis = ui.lastAccuracyScore != null,
            onViewAnalysis = onViewAnalysis,
        )
        RecordingScoreRow(ui = ui)
        TeleprompterCard(
            targetText = targetText,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        if (isAnalyzing) {
            TargetAnalyzingIndicator()
        }
        RecordingPrepAndMicStatus(ui = ui)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(feedbackScroll),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RecordingScrollableSection(ui = ui)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (!micGranted) {
            Button(
                onClick = onRequestMic,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Grant Microphone Access")
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
            RecordingActionButtonRow(
                ui = ui,
                onStartStop = onStartStop,
                onRecordingPressChanged = onRecordingPressChanged,
            )
        }
    }
}

@Composable
private fun RecordingHeaderRow(
    stageTitle: String,
    canViewAnalysis: Boolean,
    onViewAnalysis: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stageTitle,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(
            onClick = onViewAnalysis,
            enabled = canViewAnalysis,
        ) {
            Icon(Icons.Filled.QueryStats, contentDescription = null)
            Spacer(modifier = Modifier.size(4.dp))
            Text("View Analysis")
        }
    }
}

@Composable
private fun RecordingScoreRow(ui: AudioClassificationUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "SCORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = formatScoreForDisplay(ui.totalScore),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = ElectricOrange,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "HOT STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "${ui.currentHotStreak}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = if (ui.currentHotStreak > 0) ElectricOrange else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun TargetAnalyzingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = ElectricOrange,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = "AI is thinking…",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun RecordingPrepAndMicStatus(ui: AudioClassificationUiState) {
    val preparing = ui.processingState == ProcessingState.PREPARING
    val recPulse = rememberInfiniteTransition(label = "recPulse")
    val recAlpha by recPulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recAlpha",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (preparing) {
            Text(
                text = "READY...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Gold,
            )
        }
        if (ui.micLive) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BloodRed.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, BloodRed.copy(alpha = 0.6f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BloodRed.copy(alpha = recAlpha), shape = RoundedCornerShape(99.dp)),
                    )
                    Text(
                        text = "REC",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = BloodRed.copy(alpha = recAlpha),
                        letterSpacing = 2.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeleprompterCard(
    targetText: String,
    modifier: Modifier = Modifier,
) {
    val steel = Color(0xFF8C96A3)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.40f),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, steel.copy(alpha = 0.8f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "TARGET LYRICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.92f),
                ),
                modifier = Modifier.align(Alignment.TopStart),
            )
            TeleprompterText(
                text = targetText,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TeleprompterText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val minFontSize = 14.sp
    var fontSize by remember(text) { mutableStateOf(32.sp) }

    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Black,
            color = color,
            lineHeight = (fontSize.value * 1.15f).sp,
            letterSpacing = 0.5.sp,
        ),
        fontSize = fontSize,
        maxLines = 4,
        textAlign = TextAlign.Center,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize) {
                val next = (fontSize.value - 2f).coerceAtLeast(minFontSize.value)
                if (next < fontSize.value) {
                    fontSize = next.sp
                }
            }
        },
    )
}

@Composable
private fun RecordingActionButtonRow(
    ui: AudioClassificationUiState,
    onStartStop: () -> Unit,
    onRecordingPressChanged: (Boolean) -> Unit,
) {
    val analyzing = ui.processingState == ProcessingState.ANALYZING
    val preparing = ui.processingState == ProcessingState.PREPARING
    val micLive = ui.micLive
    val holdInteraction = remember { MutableInteractionSource() }
    val holdPressed by holdInteraction.collectIsPressedAsState()

    LaunchedEffect(holdPressed) {
        onRecordingPressChanged(holdPressed)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val holdContainer = when {
            preparing -> Gold
            micLive && holdPressed -> MaterialTheme.colorScheme.error
            micLive -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
        val holdContent = when {
            preparing -> Color.Black.copy(alpha = 0.88f)
            micLive -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        }
        val holdLabel = when {
            micLive && holdPressed -> "Release"
            preparing -> "Wait"
            else -> "Hold"
        }
        Button(
            onClick = {},
            enabled = !analyzing,
            interactionSource = holdInteraction,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = holdContainer,
                contentColor = holdContent,
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = holdLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    maxLines = 1,
                )
            }
        }
        val tapTitle = when {
            analyzing -> "Wait"
            micLive -> "Stop"
            preparing -> "Cancel"
            else -> "Tap"
        }
        val tapSubtitle = when {
            analyzing -> "analyzing"
            micLive -> "tap to stop"
            preparing -> "countdown"
            else -> "to record"
        }
        val tapBorder = if (preparing) Gold else MaterialTheme.colorScheme.primary
        OutlinedButton(
            onClick = {
                onStartStop()
            },
            enabled = !analyzing,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, tapBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (preparing) Gold else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = tapTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    maxLines = 1,
                )
                Text(
                    text = tapSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RecordingScrollableSection(ui: AudioClassificationUiState) {
    val analyzing = ui.processingState == ProcessingState.ANALYZING
    if (!analyzing && ui.stageFeedback != null) {
        FeedbackSection(
            fullFeedback = ui.stageFeedback!!,
            technicalScorePercent = ui.lastAccuracyScore,
        )
    }
    ui.message?.let { err ->
        Text(
            text = err,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
    TechnicalStatsExpandable(ui = ui)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun GameScoreHeader(ui: AudioClassificationUiState) {
    val isHot = ui.scoreMultiplier > 1.0
    val hotSession = ui.currentHotStreak > 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "STAGE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp,
        )
        Text(
            text = ui.currentStage.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "SCORE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isHot) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = ElectricOrange,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 4.dp),
                )
            }
            Text(
                text = formatScoreForDisplay(ui.totalScore),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    lineHeight = 76.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isHot) ElectricOrange else MaterialTheme.colorScheme.primary,
                ),
                modifier = if (isHot) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricOrange.copy(alpha = 0.28f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = size.maxDimension * 0.55f,
                            ),
                        )
                    }
                } else {
                    Modifier
                },
            )
        }

        Text(
            text = "HOT STREAK",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (hotSession) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (ui.currentHotStreak >= 6) IndustrialOrange else ElectricOrange,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(end = 6.dp),
                )
            }
            Text(
                text = "${ui.currentHotStreak}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 64.sp,
                    lineHeight = 68.sp,
                    fontWeight = FontWeight.Black,
                ),
                color = when {
                    ui.currentHotStreak >= 6 -> IndustrialOrange
                    hotSession -> ElectricOrange
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun TechnicalStatsExpandable(ui: AudioClassificationUiState) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Technical stats",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (ui.validationMode) {
                        ValidationMode.REAL_TIME -> {
                            StatLine(
                                label = "Top label",
                                value = ui.currentLabel,
                            )
                            StatLine(
                                label = "Growl confidence",
                                value = "${(ui.precisionScore.safeUnitFloat() * 100).toInt()}%",
                            )
                            if (ui.top3Labels.isNotEmpty()) {
                                Text(
                                    text = "Model probabilities",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                ui.top3Labels.forEach { ls ->
                                    Text(
                                        text = "· ${ls.label}: ${(ls.score.coerceIn(0f, 1f) * 100f).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            StatLine(label = "Player level", value = "${ui.currentLevel}")
                            StatLine(label = "Day streak", value = "${ui.streakCount}")
                            StatLine(
                                label = "Score multiplier",
                                value = formatScoreForDisplay(ui.scoreMultiplier) + "×",
                            )
                            if (ui.scoreMultiplier > 1.0) {
                                Text(
                                    text = "Next hit bonus +${formatScoreForDisplay(ui.scoreMultiplier)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BloodRed,
                                )
                            }
                        }
                        ValidationMode.RECORDING -> {
                            StatLine(label = "Player level", value = "${ui.currentLevel}")
                            StatLine(label = "Day streak", value = "${ui.streakCount}")
                            StatLine(label = "Arena combo", value = "${ui.stageCombo}")
                            ui.lastAccuracyScore?.let { acc ->
                                StatLine(
                                    label = "Last growl technique",
                                    value = "${acc.toInt()}%",
                                )
                            }
                            StatLine(
                                label = "Session state",
                                value = ui.processingState.name,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSection(
    fullFeedback: String,
    technicalScorePercent: Double?,
    modifier: Modifier = Modifier,
) {
    var isFeedbackExpanded by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(fullFeedback) {
        isFeedbackExpanded = false
        showFeedbackSheet = false
    }

    val critiqueBody = remember(fullFeedback) {
        stripGrowlTechniqueSuffix(fullFeedback).ifBlank { fullFeedback }
    }
    val teaserText = remember(critiqueBody) { feedbackTeaserTwoLines(critiqueBody) }
    val hasMoreInCard = remember(critiqueBody, teaserText) {
        teaserIsShorterThanFull(teaserText, critiqueBody)
    }

    val technical01 = remember(technicalScorePercent) {
        technicalScorePercent?.takeIf { it.isFinite() }?.div(100.0)?.coerceIn(0.0, 1.0)
    }

    if (showFeedbackSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFeedbackSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "COACH — FULL CRITIQUE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = critiqueBody,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                    onClick = { showFeedbackSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Back to Training")
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .animateContentSize(),
        ) {
            Text(
                text = "COACH",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            technical01?.let { t ->
                Text(
                    text = "Technical score: ${formatTechnicalScore01Text(t)} (${(t * 100).toInt()}%)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = ElectricOrange,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = if (isFeedbackExpanded) critiqueBody else teaserText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (hasMoreInCard) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { isFeedbackExpanded = !isFeedbackExpanded },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(if (isFeedbackExpanded) "See less" else "See more")
                        Icon(
                            imageVector = if (isFeedbackExpanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = null,
                        )
                    }
                }
                TextButton(
                    onClick = { showFeedbackSheet = true },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text("Full critique")
                }
            }
        }
    }
}

@Composable
private fun MicPrimaryButton(
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    onStartStop: () -> Unit,
    isRecording: Boolean,
    analyzing: Boolean,
    validationMode: ValidationMode,
    outlined: Boolean = false,
) {
    if (!micGranted) {
        Button(onClick = onRequestMic) {
            Text("Grant Microphone Access")
        }
        return
    }
    val label = when {
        analyzing -> "ANALYZING…"
        isRecording -> "STOP"
        validationMode == ValidationMode.RECORDING -> "START RECORDING"
        else -> "START"
    }
    val content: @Composable () -> Unit = {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
    }
    if (outlined) {
        OutlinedButton(
            onClick = onStartStop,
            enabled = !analyzing,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            content()
        }
    } else {
        Button(
            onClick = onStartStop,
            enabled = !analyzing,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(64.dp),
            shape = RoundedCornerShape(8.dp),
            colors = if (isRecording) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            },
        ) {
            content()
        }
    }
}

@Composable
private fun RageMeterBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier.height(20.dp)) {
        val stroke = 2.dp.toPx()
        val r = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        drawRoundRect(color = track, cornerRadius = r)
        val innerW = (size.width - stroke * 2) * progress
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(BloodRed, ElectricOrange, Gold)),
            topLeft = Offset(stroke, stroke),
            size = Size(
                width = innerW.coerceAtLeast(0f),
                height = size.height - stroke * 2,
            ),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
    }
}

private fun Float.safeUnitFloat(): Float {
    if (!isFinite()) return 0f
    return coerceIn(0f, 1f)
}
