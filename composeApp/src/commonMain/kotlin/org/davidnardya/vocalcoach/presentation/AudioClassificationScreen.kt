package org.davidnardya.vocalcoach.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vocalcoach.composeapp.generated.resources.Res

private const val MODEL_RESOURCE_PATH = "files/soundclassifier_with_metadata.tflite"

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AudioClassificationRoute(viewModel: AudioClassificationViewModel) {
    val mic = rememberMicrophonePermissionState()
    val ui by viewModel.state.collectAsState()

    AudioClassificationScreen(
        ui = ui,
        micGranted = mic.isGranted,
        onRequestMic = { mic.requestPermission() },
        onStartStop = {
            viewModel.onStartStopClicked(mic.isGranted) { Res.readBytes(MODEL_RESOURCE_PATH) }
        },
    )
}

@Composable
private fun AudioClassificationScreen(
    ui: AudioClassificationUiState,
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    onStartStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = ui.topLabel,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Confidence: ${(ui.topScore * 100f).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
        )
        ui.message?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = msg, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(32.dp))
        if (!micGranted) {
            Button(onClick = onRequestMic) {
                Text("Grant microphone access")
            }
        } else {
            Button(onClick = onStartStop) {
                Text(if (ui.isListening) "Stop" else "Start")
            }
        }
    }
}
