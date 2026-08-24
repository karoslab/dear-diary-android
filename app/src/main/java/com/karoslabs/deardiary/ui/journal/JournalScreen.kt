package com.karoslabs.deardiary.ui.journal

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karoslabs.deardiary.DearDiaryApp
import com.karoslabs.deardiary.domain.HeatmapCell
import com.karoslabs.deardiary.domain.JournalEntry
import com.karoslabs.deardiary.domain.StreakCalculator
import com.karoslabs.deardiary.domain.TimeFormat
import com.karoslabs.deardiary.ui.components.AppHeader
import com.karoslabs.deardiary.ui.components.ContributionHeatmap
import com.karoslabs.deardiary.ui.components.EntryCard
import com.karoslabs.deardiary.ui.components.ScreenTitle
import com.karoslabs.deardiary.ui.components.SectionLabel
import com.karoslabs.deardiary.ui.components.StatsPair
import com.karoslabs.deardiary.ui.theme.DiaryType
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors
import java.time.LocalDate
import java.time.ZoneId

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val streak: Int = 0,
    val heatmap: List<HeatmapCell> = emptyList(),
)

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as DearDiaryApp).container.repository
    val entries = repo.entries
}

@Composable
fun JournalScreen(
    onOpenEntry: (String) -> Unit,
    vm: JournalViewModel = viewModel(),
) {
    val entries by vm.entries.collectAsStateWithLifecycle(emptyList())
    val colors = LocalDiaryColors.current
    val today = remember { LocalDate.now() }
    val repo = (androidx.compose.ui.platform.LocalContext.current.applicationContext as DearDiaryApp).container.repository
    val streak = remember(entries) {
        StreakCalculator.daysKept(
            entries.map { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() },
            today,
        )
    }
    val heatmap = remember(entries) { repo.heatmap(entries, weeks = 20, today = today) }
    val grouped = remember(entries) {
        entries.groupBy { it.createdAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSortedMap(compareByDescending { it })
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        AppHeader()
        ScreenTitle("Journal")
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxSize().padding(bottom = 96.dp)) {
            item {
                StatsPair(streak = streak, entries = entries.size)
            }
            item {
                ContributionHeatmap(heatmap)
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        "No entries yet. Press to speak.",
                        style = DiaryType.preview,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    )
                }
            }
            grouped.forEach { (date, dayEntries) ->
                item(key = "h-$date") { SectionLabel(TimeFormat.sectionLabel(date, today)) }
                items(dayEntries, key = { it.id }) { entry ->
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        EntryCard(entry = entry, onClick = { onOpenEntry(entry.id) })
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
