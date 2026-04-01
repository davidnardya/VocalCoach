package org.davidnardya.vocalcoach.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberMicrophonePermissionState(): MicrophonePermissionState {
    var granted by remember { mutableStateOf(isIosMicGranted()) }
    return object : MicrophonePermissionState {
        override val isGranted: Boolean get() = granted
        override fun requestPermission() {
            AVAudioSession.sharedInstance().requestRecordPermission { ok: Boolean ->
                dispatch_async(dispatch_get_main_queue()) {
                    granted = ok
                }
            }
        }
    }
}

private fun isIosMicGranted(): Boolean {
    return when (AVAudioSession.sharedInstance().recordPermission) {
        AVAudioSessionRecordPermissionGranted -> true
        AVAudioSessionRecordPermissionDenied,
        AVAudioSessionRecordPermissionUndetermined,
        -> false
        else -> false
    }
}
