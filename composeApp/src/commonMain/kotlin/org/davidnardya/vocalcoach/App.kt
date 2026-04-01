package org.davidnardya.vocalcoach

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.davidnardya.vocalcoach.presentation.AudioClassificationRoute
import org.davidnardya.vocalcoach.presentation.rememberAudioClassificationViewModel

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = rememberAudioClassificationViewModel()
        AudioClassificationRoute(viewModel)
    }
}
