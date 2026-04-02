package org.davidnardya.vocalcoach.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

class AndroidPermissionManager(private val context: Context) : PermissionManager {
    @Composable
    override fun AskForNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                onResult(isGranted)
            }

            SideEffect {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onResult(true)
        }
    }

    override suspend fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

actual fun createPermissionManager(): PermissionManager {
    return AndroidPermissionManager(AndroidPlatformContext.application)
}
