package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.core.util.PlatformBackHandler
import org.davidnardya.vocalcoach.core.util.rememberMicrophonePermissionState
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vocalcoach.composeapp.generated.resources.Res
import kotlin.math.floor
import kotlin.math.round

private const val MODEL_RESOURCE_PATH = "files/soundclassifier_with_metadata.tflite"

/** One decimal when needed (e.g. 0.5); whole scores without a trailing ".0". */
private fun formatScoreForDisplay(value: Double): String {
    val r = round(value * 10.0) / 10.0
    val whole = floor(r).toLong()
    val frac = round((r - whole) * 10.0).toInt()
    return if (frac == 0) whole.toString() else "$whole.$frac"
}

private val ElectricOrange = Color(0xFFFF8F00)
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

    val shake = remember { Animatable(0f) }

    PlatformBackHandler(enabled = ui.isRecording) {
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
        AudioClassificationScreen(
            modifier = Modifier
                .graphicsLayer { translationX = shake.value },
            ui = ui,
            micGranted = mic.isGranted,
            snackbarHostState = snackbarHostState,
            onRequestMic = { mic.requestPermission() },
            onStartStop = {
                viewModel.onStartStopClicked(mic.isGranted) { Res.readBytes(MODEL_RESOURCE_PATH) }
            },
            onBack = {
                if (ui.isRecording) {
                    viewModel.onBackRequested()
                } else {
                    onBack()
                }
            },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioClassificationScreen(
    ui: AudioClassificationUiState,
    micGranted: Boolean,
    snackbarHostState: SnackbarHostState,
    onRequestMic: () -> Unit,
    onStartStop: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGrowl = ui.currentLabel.contains("Growl", ignoreCase = true)
    val precision by animateFloatAsState(
        targetValue = ui.precisionScore.safeUnitFloat(),
        animationSpec = tween(280, easing = LinearEasing),
        label = "precision",
    )

    val isHot = ui.scoreMultiplier > 1.0
    val multiplierPulse = rememberInfiniteTransition(label = "multPulse")
    val multScale by multiplierPulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "multScale",
    )

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
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LEVEL ${ui.currentLevel}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary,
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
                                    .size(44.dp)
                                    .padding(end = 4.dp),
                            )
                        }
                        Text(
                            text = formatScoreForDisplay(ui.totalScore),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isHot) {
                                    ElectricOrange
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            ),
                            modifier = if (isHot) {
                                Modifier.drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                ElectricOrange.copy(alpha = 0.3f),
                                                Color.Transparent,
                                            ),
                                            center = center,
                                            radius = size.maxDimension * 0.6f,
                                        ),
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                    }
                    Text(
                        text = "TOTAL SCORE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    
                    if (isHot) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .graphicsLayer {
                                    scaleX = multScale
                                    scaleY = multScale
                                }
                                .background(BloodRed, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NEXT +${formatScoreForDisplay(ui.scoreMultiplier)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = "Streak ${ui.streakCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "PRECISION METER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RageMeterBar(progress = precision, modifier = Modifier.fillMaxWidth(0.92f))

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = ui.stagePrompt,
                        style = MaterialTheme.typography.titleMedium,
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
                    
                    Text(
                        text = "Hot streak ${ui.currentHotStreak}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (ui.currentHotStreak >= 6) IndustrialOrange else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Vocal Status",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = when {
                            isGrowl -> "GROWL!"
                            ui.isRecording -> "Clean"
                            else -> "Idling"
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = if (isGrowl) BloodRed else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Heard: ${ui.currentLabel} (${(ui.precisionScore.safeUnitFloat() * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    if (!micGranted) {
                        Button(onClick = onRequestMic) {
                            Text("Grant Microphone Access")
                        }
                    } else {
                        Button(
                            onClick = onStartStop,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(64.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (ui.isRecording) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            } else {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            },
                        ) {
                            Text(
                                text = if (ui.isRecording) "STOP" else "START",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

private val IndustrialOrange = Color(0xFFFF8F00)

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
