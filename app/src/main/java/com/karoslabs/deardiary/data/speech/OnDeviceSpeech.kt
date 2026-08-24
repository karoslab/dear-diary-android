package com.karoslabs.deardiary.data.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

data class SpeechEngineInfo(
    val label: String,
    val detail: String,
    val onDeviceAvailable: Boolean,
)

class OnDeviceSpeech(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var committed = StringBuilder()
    private var lastShown = ""
    var onPartial: ((String) -> Unit)? = null

    val engineInfo: SpeechEngineInfo
        get() {
            val onDevice = Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            return if (onDevice) {
                SpeechEngineInfo(
                    label = "Google speech, on device",
                    detail = "Android may download the speech model once, from Google, so dear diary can work with no internet after that. Your voice never rides along. This app has no internet permission, so journal audio and transcripts cannot be uploaded by dear diary.",
                    onDeviceAvailable = true,
                )
            } else {
                SpeechEngineInfo(
                    label = "Android speech recognizer, prefers offline",
                    detail = "dear diary asks the system recognizer to stay offline (EXTRA_PREFER_OFFLINE). If an on-device model is not installed, recognition may be unavailable rather than sending your journal to a server. This app has no internet permission. The system may still download a speech model once, from Google, so later sessions work with no internet.",
                    onDeviceAvailable = false,
                )
            }
        }

    fun start() {
        main.post {
            stopInternal(clear = true)
            running = true
            committed.clear()
            lastShown = ""
            bindAndListen()
        }
    }

    fun pause() {
        main.post {
            running = false
            stopInternal(clear = false)
        }
    }

    fun resume() {
        main.post {
            running = true
            bindAndListen()
        }
    }

    fun stop(): String {
        val result = lastShown.ifBlank { committed.toString().trim() }
        main.post {
            running = false
            stopInternal(clear = false)
        }
        return result
    }

    fun release() {
        main.post {
            running = false
            stopInternal(clear = true)
        }
    }

    private fun bindAndListen() {
        if (!running) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onPartial?.invoke(committed.toString())
            return
        }
        if (recognizer == null) {
            recognizer = createRecognizer()
            recognizer?.setRecognitionListener(listener)
        }
        try {
            recognizer?.startListening(intent())
        } catch (_: Exception) {
            // Keep recording audio even if speech fails.
        }
    }

    private fun createRecognizer(): SpeechRecognizer {
        return if (Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private fun intent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private fun stopInternal(clear: Boolean) {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
        if (clear) {
            committed.clear()
            lastShown = ""
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onError(error: Int) {
            if (!running) return
            main.postDelayed({ if (running) bindAndListen() }, 250)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val piece = firstResult(partialResults)
            val shown = listOf(committed.toString().trim(), piece)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            lastShown = shown
            onPartial?.invoke(shown)
        }

        override fun onResults(results: Bundle?) {
            val piece = firstResult(results)
            if (piece.isNotBlank()) {
                if (committed.isNotEmpty()) committed.append(' ')
                committed.append(piece)
            }
            onPartial?.invoke(committed.toString())
            lastShown = committed.toString().trim()
            if (running) {
                main.postDelayed({ if (running) bindAndListen() }, 150)
            }
        }
    }

    private fun firstResult(bundle: Bundle?): String {
        val list = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        return list?.firstOrNull()?.trim().orEmpty()
    }
}
