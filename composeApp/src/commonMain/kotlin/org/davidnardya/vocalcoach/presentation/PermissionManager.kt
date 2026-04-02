package org.davidnardya.vocalcoach.presentation

import androidx.compose.runtime.Composable

interface PermissionManager {
    @Composable
    fun AskForNotificationPermission(onResult: (Boolean) -> Unit)
    suspend fun hasNotificationPermission(): Boolean
}

expect fun createPermissionManager(): PermissionManager
