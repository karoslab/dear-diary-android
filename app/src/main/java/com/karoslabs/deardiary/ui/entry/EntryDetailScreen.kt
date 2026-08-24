package com.karoslabs.deardiary.ui.entry

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karoslabs.deardiary.DearDiaryApp
import com.karoslabs.deardiary.data.audio.AudioPlayer
import com.karoslabs.deardiary.data.audio.PlayerSnapshot
import com.karoslabs.deardiary.domain.JournalEntry
import com.karoslabs.deardiary.domain.TimeFormat
import com.karoslabs.deardiary.ui.components.AppHeader
import com.karoslabs.deardiary.ui.components.TagChip
import com.karoslabs.deardiary.ui.theme.DiaryType
import com.karoslabs.deardiary.ui.theme.Inter
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors
import com.karoslabs.deardiary.domain.StorageNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val app = application as DearDiaryApp
    private val idFlow = MutableStateFlow(savedStateHandle.get<String>("id").orEmpty())
    private val _resolved = MutableStateFlow(false)
    val resolved = _resolved.asStateFlow()
    val entry = idFlow.flatMapLatest { id ->
        _resolved.value = false
        if (id.isBlank()) flowOf(null) else app.container.repository.observe(id)
    }.onEach { _resolved.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setId(id: String) {
        idFlow.value = id
    }

    fun saveTranscript(id: String, text: String) {
        viewModelScope.launch { app.container.repository.updateTranscript(id, text) }
    }

    fun saveTitle(id: String, text: String) {
        viewModelScope.launch { app.container.repository.updateTitle(id, text) }
    }

    fun addTag(id: String, tag: String) {
        viewModelScope.launch { app.container.repository.addTag(id, tag) }
    }

    fun removeTag(id: String, tag: String) {
        viewModelScope.launch { app.container.repository.removeTag(id, tag) }
    }

    fun delete(id: String, then: () -> Unit) {
        viewModelScope.launch {
            app.container.repository.delete(id)
            then()
        }
    }

    suspend fun exportFile(id: String): File = withContext(Dispatchers.IO) {
        val dest = File(app.container.audioStorage.exportCacheDir, StorageNames.exportZipName(id))
        app.container.repository.exportOne(id, dest)
        dest
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryDetailScreen(
    entryId: String,
    onBack: () -> Unit,
    vm: EntryDetailViewModel = viewModel(),
) {
    val colors = LocalDiaryColors.current
    val context = LocalContext.current
    val app = context.applicationContext as DearDiaryApp
    val exportScope = rememberCoroutineScope()
    LaunchedEffect(entryId) { vm.setId(entryId) }
    val entry by vm.entry.collectAsStateWithLifecycle()
    val resolved by vm.resolved.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var addingTag by remember { mutableStateOf(false) }
    var tagDraft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        AppHeader()
        val current = entry
        if (!resolved) {
            return
        }
        if (current == null) {
            Text("Entry gone.", color = colors.textSecondary, modifier = Modifier.padding(24.dp))
            return
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Delete entry",
                    color = colors.delete,
                    fontFamily = Inter,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(0.5.dp, colors.border, RoundedCornerShape(18.dp))
                        .clickable { confirmDelete = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(TimeFormat.fullStamp(current.createdAt), style = DiaryType.goldTime, color = colors.gold)
                Spacer(Modifier.weight(1f))
                Text(TimeFormat.duration(current.durationMs), style = DiaryType.goldTime, color = colors.textTertiary)
            }
            Spacer(Modifier.height(10.dp))
            var title by remember(current.id, current.title) { mutableStateOf(current.title) }
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    vm.saveTitle(current.id, it)
                },
                textStyle = DiaryType.entryTitleLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.gold),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            AudioScrubber(entry = current, storageFile = current.audioFileName?.let { app.container.audioStorage.fileFor(it) })
            Spacer(Modifier.height(22.dp))
            var body by remember(current.id, current.transcript) { mutableStateOf(current.transcript) }
            BasicTextField(
                value = body,
                onValueChange = {
                    body = it
                    vm.saveTranscript(current.id, it)
                },
                textStyle = DiaryType.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.gold),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text("Tap the words to edit", style = DiaryType.preview.copy(fontSize = 12.sp), color = colors.textTertiary)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                current.tags.forEach { tag ->
                    TagChip(tag, onRemove = { vm.removeTag(current.id, tag) })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (addingTag) {
                BasicTextField(
                    value = tagDraft,
                    onValueChange = { tagDraft = it },
                    textStyle = DiaryType.preview.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (tagDraft.isEmpty()) Text("new tag", color = colors.textTertiary, style = DiaryType.preview)
                            inner()
                        }
                    },
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Add",
                        color = colors.gold,
                        modifier = Modifier.clickable {
                            vm.addTag(current.id, tagDraft)
                            tagDraft = ""
                            addingTag = false
                        },
                    )
                    Text("Cancel", color = colors.textSecondary, modifier = Modifier.clickable { addingTag = false })
                }
            } else {
                Text(
                    "Add a tag",
                    style = DiaryType.preview.copy(fontSize = 13.sp),
                    color = colors.textTertiary,
                    modifier = Modifier.clickable { addingTag = true },
                )
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(0.5.dp, colors.border, RoundedCornerShape(18.dp))
                    .clickable {
                        exportScope.launch {
                            runCatching {
                                val file = vm.exportFile(current.id)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export this entry"))
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Export this entry", color = colors.textSecondary, fontFamily = Inter, fontSize = 15.sp)
            }
        }
    }

    if (confirmDelete) {
        val current = entry ?: return
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.7f))
                .clickable { confirmDelete = false },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .padding(22.dp),
            ) {
                Text("Delete this entry?", style = DiaryType.entryTitle, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text("The audio and the words are erased from this device. This cannot be undone.", style = DiaryType.preview, color = colors.textSecondary)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Cancel", color = colors.textSecondary, modifier = Modifier.clickable { confirmDelete = false })
                    Text(
                        "Delete",
                        color = colors.delete,
                        modifier = Modifier.clickable {
                            confirmDelete = false
                            vm.delete(current.id, onBack)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioScrubber(entry: JournalEntry, storageFile: File?) {
    val colors = LocalDiaryColors.current
    val context = LocalContext.current
    val player = remember { AudioPlayer(context) }
    var snap by remember { mutableStateOf(PlayerSnapshot(false, 0, entry.durationMs)) }
    val hasAudio = storageFile != null && storageFile.exists()

    DisposableEffect(storageFile?.absolutePath) {
        if (hasAudio) player.prepare(storageFile!!)
        onDispose { player.release() }
    }
    LaunchedEffect(hasAudio) {
        if (!hasAudio) return@LaunchedEffect
        player.positionUpdates().collect { snap = it }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clickable(enabled = hasAudio) {
                        if (snap.isPlaying) player.pause() else player.play()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (snap.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (snap.isPlaying) "Pause" else "Play",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            val duration = (if (snap.durationMs > 0) snap.durationMs else entry.durationMs).coerceAtLeast(1)
            val progress = (snap.positionMs.toFloat() / duration).coerceIn(0f, 1f)
            IosScrubber(
                progress = progress,
                enabled = hasAudio,
                onSeek = { player.seekTo((it * duration).toLong()) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth().padding(start = 40.dp, end = 4.dp, top = 2.dp)) {
            Text(TimeFormat.duration(snap.positionMs), style = DiaryType.goldTime.copy(fontSize = 10.sp), color = colors.textTertiary)
            Spacer(Modifier.weight(1f))
            Text(TimeFormat.duration(if (snap.durationMs > 0) snap.durationMs else entry.durationMs), style = DiaryType.goldTime.copy(fontSize = 10.sp), color = colors.textTertiary)
        }
        if (!hasAudio) {
            Text(
                "No audio on this entry.",
                style = DiaryType.preview.copy(fontSize = 11.sp),
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 40.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun IosScrubber(
    progress: Float,
    enabled: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDiaryColors.current
    var widthPx by remember { mutableStateOf(1f) }
    fun seekAt(x: Float) {
        if (!enabled) return
        onSeek((x / widthPx).coerceIn(0f, 1f))
    }
    Box(
        modifier
            .height(28.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled, widthPx) {
                detectTapGestures { seekAt(it.x) }
            }
            .pointerInput(enabled, widthPx) {
                detectDragGestures { change, _ -> seekAt(change.position.x) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val y = size.height / 2f
            val x = size.width * progress.coerceIn(0f, 1f)
            drawLine(
                color = colors.textTertiary.copy(alpha = 0.45f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.gold,
                start = Offset(0f, y),
                end = Offset(x, y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawRoundRect(
                color = colors.textPrimary,
                topLeft = Offset(x - 3.dp.toPx(), y - 8.dp.toPx()),
                size = Size(6.dp.toPx(), 16.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}
