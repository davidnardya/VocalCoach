package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import org.davidnardya.vocalcoach.core.util.PlatformBackHandler
import org.davidnardya.vocalcoach.core.util.rememberMicrophonePermissionState
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vocalcoach.composeapp.generated.resources.Res

private const val MODEL_RESOURCE_PATH = "files/soundclassifier_with_metadata.tflite"

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AudioClassificationRoute(
    viewModel: AudioClassificationViewModel,
    onBack: () -> Unit
) {
    val mic = rememberMicrophonePermissionState()
    val ui by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    PlatformBackHandler(enabled = ui.isRecording) {
        viewModel.onBackRequested()
    }

    LaunchedEffect(Unit) {
        viewModel.levelUpEvents.collectLatest { level ->
            snackbarHostState.showSnackbar("Level Up! You reached Level $level")
        }
    }

    if (ui.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissExitConfirmation() },
            title = { Text("Are you sure?") },
            text = { Text("Going back will stop the current recording.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDismissExitConfirmation()
                    onBack()
                }) {
                    Text("Yes, exit game")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissExitConfirmation() }) {
                    Text("Continue playing")
                }
            }
        )
    }

    AudioClassificationScreen(
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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioClassificationScreen(
    ui: AudioClassificationUiState,
    micGranted: Boolean,
    snackbarHostState: SnackbarHostState,
    onRequestMic: () -> Unit,
    onStartStop: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Score & Level
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LEVEL ${ui.currentLevel}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = ui.totalScore.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TOTAL GROWLS",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Middle: Status Indicator & Start/Stop
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isGrowl = ui.currentLabel.contains("Growl", ignoreCase = true)
                    
                    Text(
                        text = "Vocal Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isGrowl) "GROWL!" else if (ui.isRecording) "Clean" else "Idling",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = if (isGrowl) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Heard: ${ui.currentLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(48.dp))

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
                            colors = if (ui.isRecording) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(
                                text = if (ui.isRecording) "STOP" else "START",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    
                    ui.message?.let { msg ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = msg, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
                
                // Bottom Spacer to maintain layout
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}
