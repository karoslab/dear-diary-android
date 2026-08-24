package com.karoslabs.deardiary.data.audio

import java.io.File
import java.io.RandomAccessFile

/** 16-bit mono PCM WAV. Header is patched on [close]. */
class WavWriter(private val file: File, private val sampleRate: Int) {
    private val stream = file.outputStream().buffered()
    private var dataBytes = 0

    init {
        stream.write(ByteArray(HEADER))
    }

    fun write(samples: ShortArray, count: Int) {
        val n = count.coerceAtMost(samples.size)
        if (n <= 0) return
        val bytes = ByteArray(n * 2)
        var i = 0
        var b = 0
        while (i < n) {
            val s = samples[i].toInt()
            bytes[b] = (s and 0xff).toByte()
            bytes[b + 1] = ((s shr 8) and 0xff).toByte()
            i++
            b += 2
        }
        stream.write(bytes)
        dataBytes += bytes.size
    }

    fun close() {
        stream.flush()
        stream.close()
        RandomAccessFile(file, "rw").use { raf ->
            raf.write(headerBytes(sampleRate, dataBytes))
        }
    }

    companion object {
        const val HEADER = 44

        fun headerBytes(sampleRate: Int, dataBytes: Int): ByteArray {
            val out = ByteArray(HEADER)
            fun str(at: Int, s: String) {
                s.encodeToByteArray().copyInto(out, at)
            }
            fun le16(at: Int, v: Int) {
                out[at] = (v and 0xff).toByte()
                out[at + 1] = ((v shr 8) and 0xff).toByte()
            }
            fun le32(at: Int, v: Int) {
                out[at] = (v and 0xff).toByte()
                out[at + 1] = ((v shr 8) and 0xff).toByte()
                out[at + 2] = ((v shr 16) and 0xff).toByte()
                out[at + 3] = ((v shr 24) and 0xff).toByte()
            }
            str(0, "RIFF")
            le32(4, 36 + dataBytes)
            str(8, "WAVE")
            str(12, "fmt ")
            le32(16, 16)
            le16(20, 1)
            le16(22, 1)
            le32(24, sampleRate)
            le32(28, sampleRate * 2)
            le16(32, 2)
            le16(34, 16)
            str(36, "data")
            le32(40, dataBytes)
            return out
        }
    }
}
