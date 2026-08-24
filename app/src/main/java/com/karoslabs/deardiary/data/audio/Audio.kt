package com.karoslabs.deardiary.data.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import com.karoslabs.deardiary.domain.StorageNames

class AudioStorage(private val context: Context) {
    val audioDir: File
        get() = File(context.filesDir, "audio").apply { mkdirs() }

    val exportCacheDir: File
        get() = File(context.cacheDir, "export").apply { mkdirs() }

    fun tempRecording(): File = File(context.cacheDir, "recording-temp.wav")

    fun fileFor(fileName: String): File = File(audioDir, StorageNames.requireSafeBasename(fileName))

    fun newFileName(entryId: String): String = "${StorageNames.requireSafeBasename(entryId)}.wav"

    fun usedBytes(): Long {
        if (!audioDir.exists()) return 0L
        return audioDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } +
            File(context.getDatabasePath("dear_diary.db").parent ?: context.filesDir.path)
                .walkTopDown()
                .filter { it.isFile && it.name.startsWith("dear_diary") }
                .sumOf { it.length() }
    }

    fun audioBytes(): Long {
        if (!audioDir.exists()) return 0L
        return audioDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun deleteAudio(fileName: String?) {
        if (fileName.isNullOrBlank()) return
        fileFor(fileName).delete()
    }

    fun deleteAllAudio() {
        audioDir.listFiles()?.forEach { it.delete() }
        tempRecording().delete()
    }
}

class AudioRecorder(private val storage: AudioStorage) {
    private var record: android.media.AudioRecord? = null
    private var thread: Thread? = null
    private var writer: WavWriter? = null
    @Volatile private var running = false
    @Volatile private var paused = false
    var onPcm: ((ShortArray, Int) -> Unit)? = null
    val outputFile: File get() = storage.tempRecording()

    fun start(context: Context) {
        stopInternal(delete = true)
        val min = android.media.AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
        )
        if (min <= 0) error("microphone buffer unavailable")
        val rec = android.media.AudioRecord(
            android.media.MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            min * 2,
        )
        if (rec.state != android.media.AudioRecord.STATE_INITIALIZED) {
            rec.release()
            error("microphone failed to open")
        }
        outputFile.delete()
        writer = WavWriter(outputFile, SAMPLE_RATE)
        record = rec
        running = true
        paused = false
        rec.startRecording()
        thread = Thread({
            val buf = ShortArray(min)
            while (running) {
                if (paused) {
                    try { Thread.sleep(20) } catch (_: InterruptedException) { break }
                    continue
                }
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    writer?.write(buf, n)
                    onPcm?.invoke(buf, n)
                }
            }
        }, "dear-diary-pcm").also { it.start() }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun stop(): File? {
        val file = outputFile
        stopInternal(delete = false)
        return if (file.exists() && file.length() > WavWriter.HEADER) file else null
    }

    fun cancel() {
        stopInternal(delete = true)
    }

    private fun stopInternal(delete: Boolean) {
        running = false
        paused = false
        thread?.interrupt()
        try { thread?.join(500) } catch (_: InterruptedException) { }
        thread = null
        try { record?.stop() } catch (_: Exception) { }
        record?.release()
        record = null
        try { writer?.close() } catch (_: Exception) { }
        writer = null
        if (delete) outputFile.delete()
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}

class AudioPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null

    val exo: ExoPlayer
        get() {
            if (player == null) {
                player = ExoPlayer.Builder(appContext).build()
            }
            return player!!
        }

    fun prepare(file: File) {
        val p = exo
        p.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        p.prepare()
        p.playWhenReady = false
        p.seekTo(0)
    }

    fun play() {
        exo.play()
    }

    fun pause() {
        exo.pause()
    }

    fun seekTo(positionMs: Long) {
        exo.seekTo(positionMs.coerceAtLeast(0))
    }

    fun release() {
        player?.release()
        player = null
    }

    fun positionUpdates(): Flow<PlayerSnapshot> = callbackFlow {
        val p = exo
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                trySend(snapshot())
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                trySend(snapshot())
            }
        }
        p.addListener(listener)
        val ticker = android.os.Handler(android.os.Looper.getMainLooper())
        val tick = object : Runnable {
            override fun run() {
                trySend(snapshot())
                ticker.postDelayed(this, 200)
            }
        }
        ticker.post(tick)
        awaitClose {
            ticker.removeCallbacksAndMessages(null)
            p.removeListener(listener)
        }
    }

    fun snapshot(): PlayerSnapshot {
        val p = player
        return PlayerSnapshot(
            isPlaying = p?.isPlaying == true,
            positionMs = p?.currentPosition ?: 0L,
            durationMs = (p?.duration ?: 0L).coerceAtLeast(0L),
        )
    }
}

data class PlayerSnapshot(
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)
