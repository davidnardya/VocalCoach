package org.davidnardya.vocalcoach.domain.usecase.audio

/**
 * Ensures [bytes] look like a real TensorFlow Lite FlatBuffer before native code runs.
 * A 1-byte placeholder or truncated copy triggers "not a valid Flatbuffer buffer" in Task API.
 */
fun requireValidTfliteModelBytes(bytes: ByteArray) {
    if (bytes.size < MIN_TFLITE_BYTES) {
        throw IllegalArgumentException(
            "Model file is too small (${bytes.size} bytes). " +
                "Replace composeResources/files/soundclassifier_with_metadata.tflite with your full exported .tflite (with metadata).",
        )
    }
    // FlatBuffers start with a 32-bit little-endian root table offset.
    val rootOffset = readLeUInt32(bytes, 0)
    if (rootOffset < MIN_ROOT_OFFSET || rootOffset > bytes.size - MIN_TAIL_AFTER_ROOT) {
        throw IllegalArgumentException(
            "Model bytes do not look like a valid FlatBuffer/TFLite file (bad root offset). " +
                "Re-export the model or copy the correct soundclassifier_with_metadata.tflite into composeResources/files/.",
        )
    }
}

private const val MIN_TFLITE_BYTES = 256
private const val MIN_ROOT_OFFSET = 4
private const val MIN_TAIL_AFTER_ROOT = 8

private fun readLeUInt32(bytes: ByteArray, offset: Int): Int {
    require(offset + 4 <= bytes.size)
    return (bytes[offset].toInt() and 0xFF) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}
