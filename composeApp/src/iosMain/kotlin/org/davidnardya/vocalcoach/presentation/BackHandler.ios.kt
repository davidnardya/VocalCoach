package org.davidnardya.vocalcoach.presentation

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {//todo - why no use?
    // iOS swipe-to-back is usually handled by the system or navigation controller
    // In a pure Compose UI, we might need a custom solution if using a NavHost
    // For now, we'll leave it empty as standard iOS behavior doesn't have a "Back" button listener like Android.
}
