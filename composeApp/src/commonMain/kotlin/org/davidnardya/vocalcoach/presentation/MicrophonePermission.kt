package org.davidnardya.vocalcoach.presentation

import androidx.compose.runtime.Composable

interface MicrophonePermissionState {
    val isGranted: Boolean
    fun requestPermission()
}

@Composable
expect fun rememberMicrophonePermissionState(): MicrophonePermissionState
