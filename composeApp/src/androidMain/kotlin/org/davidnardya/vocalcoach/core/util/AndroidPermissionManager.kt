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
                ActivityResultContracts.RequestPermission(),
            ) { granted -> onResult(granted) }
            SideEffect {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onResult(true)
        }
    }

    override suspend fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    @Composable
    override fun AskForRecordAudioPermission(onResult: (Boolean) -> Unit) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> onResult(granted) }
        SideEffect {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override suspend fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
}

actual fun createPermissionManager(): PermissionManager =
    AndroidPermissionManager(AndroidPlatformContext.application)
