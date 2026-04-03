package org.davidnardya.vocalcoach.core.util

import androidx.compose.runtime.Composable

/**
 * RECORD_AUDIO + POST_NOTIFICATIONS (Android 13+). iOS: microphone + notification center.
 */
interface PermissionManager {
    @Composable
    fun AskForNotificationPermission(onResult: (Boolean) -> Unit)

    suspend fun hasNotificationPermission(): Boolean

    @Composable
    fun AskForRecordAudioPermission(onResult: (Boolean) -> Unit)

    suspend fun hasRecordAudioPermission(): Boolean
}

expect fun createPermissionManager(): PermissionManager
