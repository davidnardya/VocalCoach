package org.davidnardya.vocalcoach.data.audio

/**
 * Wraps 16-bit mono PCM in a minimal RIFF WAV container for Gemini multimodal input.
 */
fun encodePcm16MonoToWav(pcm: ShortArray, sampleRateHz: Int): ByteArray {
    val numChannels = 1
    val bitsPerSample = 16
    val blockAlign = numChannels * bitsPerSample / 8
    val byteRate = sampleRateHz * blockAlign
    val dataSize = pcm.size * 2
    val chunkSize = 36 + dataSize
    val out = ByteArray(44 + dataSize)
    var o = 0
    fun writeAscii(s: String) {
        for (c in s) out[o++] = c.code.toByte()
    }
    fun writeLe32(v: Int) {
        out[o++] = (v and 0xff).toByte()
        out[o++] = (v shr 8 and 0xff).toByte()
        out[o++] = (v shr 16 and 0xff).toByte()
        out[o++] = (v shr 24 and 0xff).toByte()
    }
    fun writeLe16(v: Int) {
        out[o++] = (v and 0xff).toByte()
        out[o++] = (v shr 8 and 0xff).toByte()
    }
    writeAscii("RIFF")
    writeLe32(chunkSize)
    writeAscii("WAVE")
    writeAscii("fmt ")
    writeLe32(16)
    writeLe16(1)
    writeLe16(numChannels)
    writeLe32(sampleRateHz)
    writeLe32(byteRate)
    writeLe16(blockAlign)
    writeLe16(bitsPerSample)
    writeAscii("data")
    writeLe32(dataSize)
    for (s in pcm) {
        val v = s.toInt()
        out[o++] = (v and 0xff).toByte()
        out[o++] = (v shr 8 and 0xff).toByte()
    }
    return out
}
