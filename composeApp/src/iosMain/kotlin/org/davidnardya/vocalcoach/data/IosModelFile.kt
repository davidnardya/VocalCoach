package org.davidnardya.vocalcoach.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSTemporaryDirectory
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.writeToTemporaryTfliteFile(fileName: String): String {
    val base = NSTemporaryDirectory().toString().trimEnd('/')
    val path = "$base/$fileName"
    if (isEmpty()) return path
    val fp = fopen(path, "wb") ?: error("fopen failed for $path")
    try {
        this.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1u, size.convert(), fp)
        }
    } finally {
        fclose(fp)
    }
    return path
}
