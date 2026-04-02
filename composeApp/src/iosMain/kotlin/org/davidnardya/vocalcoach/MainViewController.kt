@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

package org.davidnardya.vocalcoach

import androidx.compose.ui.window.ComposeUIViewController
import kotlin.native.ObjCName
import org.davidnardya.vocalcoach.presentation.navigation.App
import platform.UIKit.UIViewController

/** Swift: `ComposeUIViewFactory.shared.create()` */
@ObjCName("ComposeUIViewFactory", exact = true)
object ComposeUIViewFactory {
    fun create(): UIViewController = ComposeUIViewController {
        App()
    }
}
