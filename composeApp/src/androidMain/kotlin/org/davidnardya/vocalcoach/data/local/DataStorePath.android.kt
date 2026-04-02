package org.davidnardya.vocalcoach.data.local

import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

actual fun producePath(name: String): String {
    return AndroidPlatformContext.application.filesDir.resolve(name).absolutePath
}
