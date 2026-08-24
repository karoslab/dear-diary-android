package com.karoslabs.deardiary.ui.settings

import android.app.Application
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karoslabs.deardiary.BuildConfig
import com.karoslabs.deardiary.DearDiaryApp
import com.karoslabs.deardiary.data.repository.DeviceStorageStats
import com.karoslabs.deardiary.data.speech.SpeechEngineInfo
import com.karoslabs.deardiary.domain.ThemeMode
import com.karoslabs.deardiary.domain.TimeFormat
import com.karoslabs.deardiary.ui.components.AppHeader
import com.karoslabs.deardiary.ui.components.DiaryCard
import com.karoslabs.deardiary.ui.components.ScreenTitle
import com.karoslabs.deardiary.ui.theme.DiaryType
import com.karoslabs.deardiary.ui.theme.Inter
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DearDiaryApp
    val themeMode = app.container.userPrefs.themeMode
    val engine: SpeechEngineInfo = app.container.speech.engineInfo
    private val _stats = MutableStateFlow<DeviceStorageStats?>(null)
    val stats = _stats.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _stats.value = app.container.repository.deviceStats()
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { app.container.userPrefs.setThemeMode(mode) }
    }

    suspend fun exportFile(): File {
        val dest = File(app.container.audioStorage.exportCacheDir, "dear-diary-backup.zip")
        app.container.repository.exportEverything(dest)
        return dest
    }

    fun importBytes(bytes: ByteArray) {
        viewModelScope.launch {
            runCatching {
                app.container.repository.importBackup(bytes.inputStream())
                refreshStats()
                _message.value = "Imported. Everything stayed on this device."
            }.onFailure {
                _message.value = "Could not import that file."
            }
        }
    }

    fun wipe() {
        viewModelScope.launch {
            app.container.repository.wipeEverything()
            refreshStats()
            _message.value = "Everything on this device was erased."
        }
    }

    fun seedDebug() {
        viewModelScope.launch {
            app.container.repository.seedSampleJournal()
            refreshStats()
            _message.value = "Sample journal loaded on this device only."
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val colors = LocalDiaryColors.current
    val context = LocalContext.current
    val theme by vm.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
    val stats by vm.stats.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var confirmWipe by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { vm.importBytes(it.readBytes()) }
        }
    }

    LaunchedEffect(Unit) { vm.refreshStats() }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        AppHeader()
        ScreenTitle("Settings")
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 120.dp),
        ) {
            message?.let {
                Text(it, color = colors.gold, style = DiaryType.preview, modifier = Modifier.padding(bottom = 12.dp).clickable { vm.clearMessage() })
            }
            SectionTitle("YOUR DATA")
            DiaryCard {
                SettingsRow(
                    title = "Export everything",
                    subtitle = "Save a full backup, one file with your words and your audio, to this device.",
                    onClick = {
                        scope.launch {
                            runCatching {
                                val file = vm.exportFile()
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export everything"))
                            }
                        }
                    },
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)
                SettingsRow(
                    title = "Import",
                    subtitle = "Bring in a backup file that is already on this device.",
                    onClick = {
                        import.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                            ),
                        )
                    },
                )
            }
            Spacer(Modifier.height(22.dp))
            SectionTitle("ON THIS DEVICE")
            DiaryCard {
                val s = stats
                val usedRatio = if (s == null || s.totalBytes == 0L) 0f else {
                    1f - (s.freeBytes.toFloat() / s.totalBytes.toFloat())
                }
                Column(Modifier.padding(vertical = 16.dp)) {
                    StorageBar(usedRatio.coerceIn(0f, 1f))
                    Spacer(Modifier.height(12.dp))
                    val free = s?.freeBytes ?: deviceFree()
                    val total = s?.totalBytes ?: deviceTotal()
                    Text(
                        "${TimeFormat.bytes(free)} free of ${TimeFormat.bytes(total)} on this device",
                        style = DiaryType.mono,
                        color = colors.textSecondary,
                    )
                    Text(
                        "${TimeFormat.bytes(s?.appUsedBytes ?: 0)} used by dear diary",
                        style = DiaryType.mono,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("${s?.entryCount ?: 0} entries", style = DiaryType.mono, color = colors.textSecondary)
                        Spacer(Modifier.weight(1f))
                        Text("${TimeFormat.bytes(s?.audioBytes ?: 0)} of audio", style = DiaryType.mono, color = colors.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionTitle("THEME")
            DiaryCard {
                Column(Modifier.padding(vertical = 14.dp)) {
                    ThemeSegment(selected = theme, onSelect = vm::setTheme)
                }
            }
            Text(
                "dear diary follows your device setting for reduced motion. When it is on, animations are kept to a minimum.",
                style = DiaryType.preview.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(10.dp))
            SectionTitle("SPEECH TO TEXT")
            DiaryCard {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Model", color = colors.textPrimary, fontFamily = Inter)
                    Spacer(Modifier.weight(1f))
                    Text(vm.engine.label, color = colors.textSecondary, style = DiaryType.preview)
                }
            }
            Text(
                vm.engine.detail,
                style = DiaryType.preview.copy(fontSize = 12.sp),
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(16.dp))
            DiaryCard {
                SettingsRow(
                    title = "Wipe everything",
                    subtitle = "Erase every entry, every tag, and every audio file on this device. Your journal is yours to destroy too.",
                    titleColor = colors.delete,
                    onClick = { confirmWipe = true },
                )
            }
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(22.dp))
                DiaryCard {
                    SettingsRow(
                        title = "Load sample journal",
                        subtitle = "Debug only. Adds the screenshot entries on this device.",
                        onClick = vm::seedDebug,
                    )
                }
            }
        }
    }

    if (confirmWipe) {
        Box(
            Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.72f)).clickable { confirmWipe = false },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(28.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .padding(22.dp),
            ) {
                Text("Wipe everything?", style = DiaryType.entryTitle, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text("This deletes every word and every recording stored by dear diary on this phone. There is no cloud copy.", style = DiaryType.preview, color = colors.textSecondary)
                Spacer(Modifier.height(16.dp))
                Row {
                    Text("Cancel", color = colors.textSecondary, modifier = Modifier.clickable { confirmWipe = false })
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Wipe",
                        color = colors.delete,
                        modifier = Modifier.clickable {
                            confirmWipe = false
                            vm.wipe()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = LocalDiaryColors.current
    Text(
        text,
        style = DiaryType.section,
        color = colors.textTertiary,
        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
    )
}

@Composable
private fun StorageBar(usedRatio: Float) {
    val colors = LocalDiaryColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.surfaceRaised),
    ) {
        Box(
            Modifier
                .fillMaxWidth(usedRatio.coerceIn(0.02f, 1f))
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.gold),
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = LocalDiaryColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Text(title, color = titleColor ?: colors.textPrimary, fontFamily = Inter)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = DiaryType.preview, color = colors.textSecondary)
    }
}

@Composable
private fun ThemeSegment(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = LocalDiaryColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (colors.isDark) Color.Black else colors.background)
            .padding(3.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            val on = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) colors.surfaceRaised else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(mode.name, color = if (on) colors.textPrimary else colors.textSecondary, fontFamily = Inter, fontSize = 14.sp)
            }
        }
    }
}

private fun deviceFree(): Long = StatFs(Environment.getDataDirectory().absolutePath).availableBytes
private fun deviceTotal(): Long = StatFs(Environment.getDataDirectory().absolutePath).totalBytes
