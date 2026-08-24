package com.karoslabs.deardiary.ui.record

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karoslabs.deardiary.DearDiaryApp
import com.karoslabs.deardiary.data.audio.AudioRecorder
import com.karoslabs.deardiary.data.speech.OnDeviceSpeech
import com.karoslabs.deardiary.domain.JournalEntry
import com.karoslabs.deardiary.domain.ReflectionPrompts
import com.karoslabs.deardiary.domain.TimeFormat
import com.karoslabs.deardiary.ui.components.AppHeader
import com.karoslabs.deardiary.ui.theme.DiaryType
import com.karoslabs.deardiary.ui.theme.Inter
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors
import com.karoslabs.deardiary.ui.theme.LocalReduceMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

enum class RecordPhase { Idle, Recording, Paused, Saving }

data class RecordUiState(
    val phase: RecordPhase = RecordPhase.Idle,
    val elapsedMs: Long = 0,
    val liveTranscript: String = "",
    val error: String? = null,
    val saved: JournalEntry? = null,
    val reflection: String? = null,
)

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DearDiaryApp
    private val recorder = AudioRecorder(app.container.audioStorage)
    private val speech = OnDeviceSpeech(application)
    private val _state = MutableStateFlow(RecordUiState())
    val state = _state.asStateFlow()
    private var timerJob: Job? = null
    private val finishing = AtomicBoolean(false)

    init {
        speech.onPartial = { text ->
            _state.update { it.copy(liveTranscript = text) }
        }
    }

    fun start(context: android.content.Context) {
        if (_state.value.phase != RecordPhase.Idle) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { speech.start() }
                recorder.onPcm = { samples, n -> speech.acceptPcm(samples, n) }
                recorder.start(context)
                _state.update {
                    it.copy(phase = RecordPhase.Recording, elapsedMs = 0, liveTranscript = "", error = null, saved = null, reflection = null)
                }
                startTimer()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Could not start the microphone.") }
            }
        }
    }

    fun pause() {
        recorder.pause()
        speech.pause()
        timerJob?.cancel()
        _state.update { it.copy(phase = RecordPhase.Paused) }
    }

    fun resume() {
        recorder.resume()
        speech.resume()
        _state.update { it.copy(phase = RecordPhase.Recording) }
        startTimer()
    }

    fun discard() {
        timerJob?.cancel()
        speech.release()
        recorder.cancel()
        _state.value = RecordUiState()
    }

    fun finish() {
        val phase = _state.value.phase
        if (phase != RecordPhase.Recording && phase != RecordPhase.Paused) return
        if (!finishing.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                timerJob?.cancel()
                val elapsed = _state.value.elapsedMs
                _state.update { it.copy(phase = RecordPhase.Saving) }
                val file: File? = recorder.stop()
                val stopped = speech.stop()
                val shown = _state.value.liveTranscript
                val transcript = if (shown.length >= stopped.length) shown else stopped
                if (elapsed < 400 && transcript.isBlank() && file == null) {
                    _state.update { it.copy(phase = RecordPhase.Idle, error = "Nothing to save yet.") }
                    return@launch
                }
                val entry = app.container.repository.saveNew(
                    transcript = transcript,
                    durationMs = elapsed,
                    tempAudio = file,
                )
                _state.update {
                    it.copy(
                        phase = RecordPhase.Idle,
                        elapsedMs = 0,
                        liveTranscript = "",
                        saved = entry,
                        reflection = ReflectionPrompts.forEntry(entry.id),
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(phase = RecordPhase.Idle, error = "Could not save that entry.") }
            } finally {
                finishing.set(false)
            }
        }
    }

    fun dismissReflection() {
        _state.update { it.copy(reflection = null, saved = null) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(200)
                _state.update { it.copy(elapsedMs = it.elapsedMs + 200) }
            }
        }
    }

    override fun onCleared() {
        speech.release()
        recorder.cancel()
        super.onCleared()
    }
}

@Composable
fun RecordScreen(
    onOpenEntry: (String) -> Unit,
    vm: RecordViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = LocalDiaryColors.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.start(context) else {
            /* stay idle */
        }
    }

    fun onMic() {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        when {
            !ok -> permission.launch(Manifest.permission.RECORD_AUDIO)
            state.phase == RecordPhase.Idle -> vm.start(context)
            state.phase == RecordPhase.Recording -> vm.finish()
            state.phase == RecordPhase.Paused -> vm.finish()
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp)) {
                    RecordButton(
                        phase = state.phase,
                        onClick = { onMic() },
                    )
                    Spacer(Modifier.height(18.dp))
                    val label = when (state.phase) {
                        RecordPhase.Idle -> "PRESS TO SPEAK"
                        RecordPhase.Recording -> "LISTENING  ·  ${TimeFormat.duration(state.elapsedMs)}"
                        RecordPhase.Paused -> "PAUSED  ·  ${TimeFormat.duration(state.elapsedMs)}"
                        RecordPhase.Saving -> "SAVING"
                    }
                    Text(
                        label,
                        color = if (state.phase == RecordPhase.Idle) colors.textSecondary else colors.textPrimary,
                        fontFamily = Inter,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                        fontSize = 12.sp,
                        letterSpacing = 4.2.sp,
                    )
                    if (state.phase == RecordPhase.Recording || state.phase == RecordPhase.Paused) {
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SmallAction(
                                text = if (state.phase == RecordPhase.Paused) "Resume" else "Pause",
                                icon = if (state.phase == RecordPhase.Paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                            ) {
                                if (state.phase == RecordPhase.Paused) vm.resume() else vm.pause()
                            }
                            SmallAction(text = "Save") { vm.finish() }
                        }
                        TextButton(onClick = vm::discard) {
                            Text("Discard", color = colors.delete, fontFamily = Inter)
                        }
                    }
                    if (state.liveTranscript.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            state.liveTranscript,
                            style = DiaryType.body,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = colors.delete, style = DiaryType.preview, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        state.reflection?.let { prompt ->
            ReflectionSheet(
                prompt = prompt,
                onDismiss = vm::dismissReflection,
                onOpen = {
                    val id = state.saved?.id
                    vm.dismissReflection()
                    if (id != null) onOpenEntry(id)
                },
            )
        }
    }
}

@Composable
private fun RecordButton(phase: RecordPhase, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    val reduce = LocalReduceMotion.current
    val active = phase == RecordPhase.Recording
    val pulse = if (reduce || !active) {
        1f
    } else {
        val t = rememberInfiniteTransition(label = "rings")
        val a by t.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
            label = "p",
        )
        a
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
        if (!reduce) {
            listOf(196.dp, 168.dp, 140.dp).forEachIndexed { i, ring ->
                Box(
                    Modifier
                        .size(ring)
                        .scale(if (active) pulse else 1f)
                        .border(
                            1.dp,
                            colors.textTertiary.copy(alpha = 0.22f - i * 0.04f),
                            CircleShape,
                        ),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (active) Icons.Filled.Mic else Icons.Outlined.Mic,
                contentDescription = "Record",
                tint = if (active) colors.gold else colors.textPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun SmallAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = colors.textPrimary, fontFamily = Inter, fontSize = 14.sp)
    }
}

@Composable
private fun ReflectionSheet(prompt: String, onDismiss: () -> Unit, onOpen: () -> Unit) {
    val colors = LocalDiaryColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.surface)
                .clickable(enabled = false) {}
                .padding(24.dp)
                .padding(bottom = 88.dp),
        ) {
            Text("A quiet question", style = DiaryType.label, color = colors.gold)
            Spacer(Modifier.height(10.dp))
            Text(prompt, style = DiaryType.entryTitle, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text(ReflectionPrompts.HONESTY_NOTE, style = DiaryType.preview, color = colors.textSecondary)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Keep writing",
                    color = colors.textPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceRaised)
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    fontFamily = Inter,
                )
                Text(
                    "Done",
                    color = colors.gold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    fontFamily = Inter,
                )
            }
        }
    }
}
