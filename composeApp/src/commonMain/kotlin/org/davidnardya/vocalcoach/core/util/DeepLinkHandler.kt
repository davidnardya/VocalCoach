package org.davidnardya.vocalcoach.core.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object DeepLinkHandler {
    private val _deepLinkFlow = MutableStateFlow<String?>(null)
    val deepLinkFlow = _deepLinkFlow.asStateFlow()

    fun onNewDeepLink(url: String) {
        _deepLinkFlow.value = url
    }

    fun clearDeepLink() {
        _deepLinkFlow.value = null
    }
}
