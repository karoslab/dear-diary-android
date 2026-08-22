package com.karoslabs.deardiary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karoslabs.deardiary.domain.HeatmapCell
import com.karoslabs.deardiary.domain.JournalEntry
import com.karoslabs.deardiary.domain.TimeFormat
import com.karoslabs.deardiary.ui.navigation.AppTab
import com.karoslabs.deardiary.ui.theme.DiaryType
import com.karoslabs.deardiary.ui.theme.Inter
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = { PrivacyPill() },
) {
    val colors = LocalDiaryColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "dear diary",
            style = DiaryType.brand,
            color = colors.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun PrivacyPill(bytesLabel: String = "0 bytes sent") {
    val colors = LocalDiaryColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
            .border(0.5.dp, colors.border, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.gold),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = bytesLabel,
            style = DiaryType.mono.copy(fontSize = 10.sp),
            color = colors.textSecondary,
        )
    }
}

@Composable
fun ScreenTitle(title: String) {
    val colors = LocalDiaryColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = DiaryType.screenTitle, color = colors.textPrimary)
    }
}

@Composable
fun FloatingTabBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDiaryColors.current
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(colors.tabBar)
            .border(0.5.dp, colors.tabBarBorder, RoundedCornerShape(36.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AppTab.entries.forEach { tab ->
            TabItem(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: AppTab, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalDiaryColors.current
    val tint = if (selected) colors.gold else colors.tabInactive
    val (outlined, filled) = when (tab) {
        AppTab.Record -> Icons.Outlined.Mic to Icons.Filled.Mic
        AppTab.Journal -> Icons.AutoMirrored.Outlined.MenuBook to Icons.AutoMirrored.Filled.MenuBook
        AppTab.Search -> Icons.Outlined.Search to Icons.Outlined.Search
        AppTab.Settings -> Icons.Outlined.Settings to Icons.Outlined.Settings
    }
    val icon: ImageVector = if (selected) filled else outlined
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) colors.tabHighlight else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            tab.label,
            color = tint,
            fontFamily = Inter,
            fontSize = 10.sp,
            letterSpacing = 0.1.sp,
        )
    }
}

@Composable
fun StatsPair(streak: Int, entries: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            value = streak.toString(),
            unit = "DAY",
            caption = "STREAK KEPT",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = entries.toString(),
            unit = "ENTRIES",
            caption = "ALL ON DEVICE",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(value: String, unit: String, caption: String, modifier: Modifier = Modifier) {
    val colors = LocalDiaryColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(0.5.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(value, style = DiaryType.stat, color = colors.textPrimary)
        Text(
            unit,
            style = DiaryType.label.copy(fontSize = 11.sp, letterSpacing = 1.8.sp),
            color = colors.gold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            caption,
            style = DiaryType.label.copy(fontSize = 9.sp, letterSpacing = 1.7.sp),
            color = colors.textTertiary,
        )
    }
}

@Composable
fun ContributionHeatmap(cells: List<HeatmapCell>, modifier: Modifier = Modifier) {
    val colors = LocalDiaryColors.current
    val byDate = cells.associateBy { it.date }
    val dates = cells.map { it.date }
    if (dates.isEmpty()) return
    val min = dates.min()
    val max = dates.max()
    val weeks = mutableListOf<List<HeatmapCell?>>()
    var cursor = min
    val column = mutableListOf<HeatmapCell?>()
    repeat(min.dayOfWeek.value % 7) { column += null }
    while (!cursor.isAfter(max)) {
        column += byDate[cursor]
        if (column.size == 7) {
            weeks += column.toList()
            column.clear()
        }
        cursor = cursor.plusDays(1)
    }
    if (column.isNotEmpty()) {
        while (column.size < 7) column += null
        weeks += column
    }
    Column(modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 3.dp
            val weekCount = weeks.size.coerceAtLeast(1)
            val cell = ((maxWidth - gap * (weekCount - 1)) / weekCount).coerceIn(8.dp, 13.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        week.forEach { cellData ->
                            val level = cellData?.level ?: 0
                            Box(
                                Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(colors.heatmap.getOrElse(level) { colors.heatmap.first() }),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("less", style = DiaryType.preview.copy(fontSize = 11.sp), color = colors.textTertiary)
            Spacer(Modifier.width(8.dp))
            colors.heatmap.forEach { c ->
                Box(
                    Modifier
                        .padding(end = 3.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(c),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("more", style = DiaryType.preview.copy(fontSize = 11.sp), color = colors.textTertiary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleOverride: AnnotatedString? = null,
    previewOverride: AnnotatedString? = null,
    grouped: Boolean = false,
) {
    val colors = LocalDiaryColors.current
    val shape = if (grouped) RoundedCornerShape(0.dp) else RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (grouped) androidx.compose.ui.graphics.Color.Transparent else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    TimeFormat.timeOfDay(entry.createdAt),
                    style = DiaryType.goldTime,
                    color = colors.gold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    TimeFormat.duration(entry.durationMs),
                    style = DiaryType.goldTime,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(6.dp))
            if (titleOverride != null) {
                Text(
                    titleOverride,
                    style = DiaryType.entryTitle.copy(color = colors.textPrimary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    entry.title,
                    style = DiaryType.entryTitle,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.transcript.isNotBlank() || previewOverride != null) {
                Spacer(Modifier.height(4.dp))
                if (previewOverride != null) {
                    Text(
                        previewOverride,
                        style = DiaryType.preview.copy(color = colors.textSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        entry.transcript,
                        style = DiaryType.preview,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    entry.tags.forEach { TagChip(it) }
                }
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun TagChip(name: String, onRemove: (() -> Unit)? = null) {
    val colors = LocalDiaryColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.pill)
            .then(
                if (onRemove != null) Modifier.border(0.5.dp, colors.border, RoundedCornerShape(50))
                else Modifier,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = colors.textSecondary, fontSize = 12.sp, fontFamily = Inter)
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Remove $name",
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(12.dp)
                    .clickable(onClick = onRemove),
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    val colors = LocalDiaryColors.current
    Text(
        text = text,
        style = DiaryType.section,
        color = colors.textTertiary,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun DiaryCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalDiaryColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) { content() }
}
