package com.karoslabs.deardiary.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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

    fun tempRecording(): File = File(context.cacheDir, "recording-temp.m4a")

    fun fileFor(fileName: String): File = File(audioDir, StorageNames.requireSafeBasename(fileName))

    fun newFileName(entryId: String): String = "${StorageNames.requireSafeBasename(entryId)}.m4a"

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
    private var recorder: MediaRecorder? = null
    val outputFile: File get() = storage.tempRecording()

    fun start(context: Context) {
        stopInternal(delete = false)
        outputFile.delete()
        val rec = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(128_000)
        rec.setAudioSamplingRate(44_100)
        rec.setOutputFile(outputFile.absolutePath)
        rec.prepare()
        rec.start()
        recorder = rec
    }

    fun pause() {
        recorder?.pause()
    }

    fun resume() {
        recorder?.resume()
    }

    fun stop(): File? {
        val file = outputFile
        stopInternal(delete = false)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun cancel() {
        stopInternal(delete = true)
    }

    private fun stopInternal(delete: Boolean) {
        try {
            recorder?.stop()
        } catch (_: RuntimeException) {
            // stop() throws if start() never successfully wrote a frame
        }
        recorder?.reset()
        recorder?.release()
        recorder = null
        if (delete) outputFile.delete()
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
