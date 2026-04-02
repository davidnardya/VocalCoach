package org.davidnardya.vocalcoach.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {//todo - why no use?
    BackHandler(enabled = enabled, onBack = onBack)
}
