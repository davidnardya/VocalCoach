package org.davidnardya.vocalcoach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.davidnardya.vocalcoach.core.util.DeepLinkHandler
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext
import org.davidnardya.vocalcoach.presentation.navigation.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidPlatformContext.init(this)

        handleIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.toString()?.let { url ->
            DeepLinkHandler.onNewDeepLink(url)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
