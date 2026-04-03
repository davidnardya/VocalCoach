package org.davidnardya.vocalcoach.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

class IosPermissionManager : PermissionManager {

    @Composable
    override fun AskForNotificationPermission(onResult: (Boolean) -> Unit) {
        LaunchedEffect(Unit) {
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(granted)
                }
            }
        }
    }

    override suspend fun hasNotificationPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.getNotificationSettingsWithCompletionHandler { settings ->
                val isAuthorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
                continuation.resume(isAuthorized)
            }
        }

    @Composable
    override fun AskForRecordAudioPermission(onResult: (Boolean) -> Unit) {
        LaunchedEffect(Unit) {
            AVAudioSession.sharedInstance().requestRecordPermission { granted: Boolean ->
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(granted)
                }
            }
        }
    }

    override suspend fun hasRecordAudioPermission(): Boolean =
        when (AVAudioSession.sharedInstance().recordPermission) {
            AVAudioSessionRecordPermissionGranted -> true
            AVAudioSessionRecordPermissionDenied,
            AVAudioSessionRecordPermissionUndetermined,
            -> false
            else -> false
        }
}

actual fun createPermissionManager(): PermissionManager = IosPermissionManager()
