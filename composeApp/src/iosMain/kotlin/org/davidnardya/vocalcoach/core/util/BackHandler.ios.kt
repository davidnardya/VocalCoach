package org.davidnardya.vocalcoach.core.util

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS swipe-to-back is usually handled by the system or navigation controller
}
