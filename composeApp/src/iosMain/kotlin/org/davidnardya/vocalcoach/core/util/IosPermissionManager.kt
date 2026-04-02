package org.davidnardya.vocalcoach.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.*
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_async
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosPermissionManager : PermissionManager {
    @Composable
    override fun AskForNotificationPermission(onResult: (Boolean) -> Unit) {
        LaunchedEffect(Unit) {
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
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
}

actual fun createPermissionManager(): PermissionManager = IosPermissionManager()
