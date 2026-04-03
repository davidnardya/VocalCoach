package org.davidnardya.vocalcoach.core.util

import androidx.compose.runtime.Composable

/**
 * RECORD_AUDIO + POST_NOTIFICATIONS (Android 13+). iOS: microphone + notification center.
 * Speech recognition: Android uses the mic for [android.speech.SpeechRecognizer]; iOS uses
 * [SFSpeechRecognizer] and requires a separate authorization from microphone.
 */
interface PermissionManager {
    @Composable
    fun AskForNotificationPermission(onResult: (Boolean) -> Unit)

    suspend fun hasNotificationPermission(): Boolean

    @Composable
    fun AskForRecordAudioPermission(onResult: (Boolean) -> Unit)

    suspend fun hasRecordAudioPermission(): Boolean

    /** iOS: [SFSpeechRecognizer] authorization. Android: true when [hasRecordAudioPermission]. */
    suspend fun hasSpeechRecognitionPermission(): Boolean

    @Composable
    fun AskForSpeechRecognitionPermission(onResult: (Boolean) -> Unit)
}

expect fun createPermissionManager(): PermissionManager
