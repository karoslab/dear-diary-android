package com.karoslabs.deardiary.data.speech

import android.content.Context
import com.karoslabs.deardiary.domain.VoskJson
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

data class SpeechEngineInfo(
    val label: String,
    val detail: String,
    val onDeviceAvailable: Boolean,
)

/**
 * Live transcription that does NOT open the microphone.
 * [acceptPcm] is fed from the single AudioRecord tap that also writes the file
 * (same shape as iOS AVAudioEngine → SpeechAnalyzer + AVAudioFile).
 */
class OnDeviceSpeech(private val context: Context) {
    private val lock = Any()
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private val committed = StringBuilder()
    private var lastShown = ""
    private val started = AtomicBoolean(false)
    var onPartial: ((String) -> Unit)? = null

    val engineInfo: SpeechEngineInfo
        get() {
            val ready = synchronized(lock) { model != null || modelDir().let { File(it, "am/final.mdl").exists() } }
            return if (ready || assetZipExists()) {
                SpeechEngineInfo(
                    label = "Vosk, on this device",
                    detail = "Words are recognized from the same microphone tap that writes your audio file. The English model lives on this phone. dear diary has no internet permission, so nothing is uploaded.",
                    onDeviceAvailable = true,
                )
            } else {
                SpeechEngineInfo(
                    label = "Speech model missing",
                    detail = "The on-device speech model is not in the app package. Audio still records. Rebuild with the Vosk model asset, or recording will save sound without live words.",
                    onDeviceAvailable = false,
                )
            }
        }

    fun start() {
        synchronized(lock) {
            committed.setLength(0)
            lastShown = ""
            started.set(true)
            val m = model ?: loadModel().also { model = it }
            recognizer?.close()
            recognizer = if (m != null) Recognizer(m, SAMPLE_RATE.toFloat()) else null
        }
    }

    fun acceptPcm(samples: ShortArray, count: Int) {
        if (!started.get()) return
        val rec: Recognizer
        synchronized(lock) {
            rec = recognizer ?: return
        }
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
        val shown = synchronized(lock) {
            if (!started.get() || recognizer !== rec) return
            if (rec.acceptWaveForm(bytes, bytes.size)) {
                val piece = VoskJson.text(rec.result)
                if (piece.isNotBlank()) {
                    if (committed.isNotEmpty()) committed.append(' ')
                    committed.append(piece)
                }
                lastShown = committed.toString()
            } else {
                val partial = VoskJson.partial(rec.partialResult)
                lastShown = listOf(committed.toString(), partial)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
            }
            lastShown
        }
        onPartial?.invoke(shown)
    }

    fun pause() {
        started.set(false)
    }

    fun resume() {
        started.set(true)
    }

    fun stop(): String {
        started.set(false)
        return synchronized(lock) {
            val rec = recognizer
            if (rec != null) {
                val piece = VoskJson.text(rec.finalResult)
                if (piece.isNotBlank()) {
                    if (committed.isNotEmpty()) committed.append(' ')
                    committed.append(piece)
                }
                lastShown = committed.toString().trim()
                rec.close()
                recognizer = null
            }
            lastShown.ifBlank { committed.toString().trim() }
        }
    }

    fun release() {
        started.set(false)
        synchronized(lock) {
            recognizer?.close()
            recognizer = null
            committed.setLength(0)
            lastShown = ""
        }
    }

    private fun assetZipExists(): Boolean = try {
        context.assets.open(ASSET_ZIP).close()
        true
    } catch (_: Exception) {
        false
    }

    private fun modelDir(): File = File(context.filesDir, MODEL_DIR)

    private fun loadModel(): Model? {
        val dir = unpackModel() ?: return null
        return runCatching { Model(dir.absolutePath) }.getOrNull()
    }

    private fun unpackModel(): File? {
        val dest = modelDir()
        val unpacked = File(dest, INNER)
        val marker = File(dest, ".ok")
        if (marker.exists() && File(unpacked, "am/final.mdl").exists()) return unpacked
        dest.deleteRecursively()
        dest.mkdirs()
        val root = dest.canonicalFile
        return try {
            context.assets.open(ASSET_ZIP).use { raw ->
                ZipInputStream(raw).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.trimStart('/')
                        val out = File(dest, name).canonicalFile
                        if (!out.path.startsWith(root.path + File.separator) && out != root) {
                            throw IllegalArgumentException("zip slip: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile?.mkdirs()
                            out.outputStream().use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            if (!File(unpacked, "am/final.mdl").exists()) return null
            marker.writeText("ok")
            unpacked
        } catch (_: Exception) {
            dest.deleteRecursively()
            null
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val ASSET_ZIP = "vosk-small-en-us.zip"
        private const val MODEL_DIR = "vosk-small-en-us"
        private const val INNER = "vosk-model-small-en-us-0.15"
    }
}
