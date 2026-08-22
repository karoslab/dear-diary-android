package com.karoslabs.deardiary.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
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
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors
import com.karoslabs.deardiary.ui.theme.LocalReduceMotion
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
    val reduce = LocalReduceMotion.current
    val pulse = if (reduce) {
        1f
    } else {
        val t = rememberInfiniteTransition(label = "privacy-dot")
        val a by t.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(1400, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "dot",
        )
        a
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.pill)
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(colors.gold.copy(alpha = pulse)),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = bytesLabel,
            style = DiaryType.mono.copy(fontSize = 11.sp),
            color = colors.textSecondary,
        )
    }
}

@Composable
fun ScreenTitle(title: String) {
    val colors = LocalDiaryColors.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp)
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(colors.tabBar)
            .border(1.dp, colors.tabBarBorder, RoundedCornerShape(38.dp))
            .padding(horizontal = 8.dp),
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
    val tint = if (selected) colors.gold else colors.textSecondary
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
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .then(
                    if (selected) {
                        Modifier.drawBehind {
                            drawCircle(
                                color = colors.gold.copy(alpha = 0.16f),
                                radius = size.minDimension / 1.6f,
                            )
                        }
                    } else Modifier,
                ),
        ) {
            Icon(icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            tab.label,
            color = tint,
            fontFamily = com.karoslabs.deardiary.ui.theme.Inter,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun StatsPair(streak: Int, entries: Int) {
    val colors = LocalDiaryColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(value, style = DiaryType.stat, color = colors.textPrimary)
        Text(unit, style = DiaryType.label, color = colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Text(caption, style = DiaryType.label.copy(fontSize = 10.sp, letterSpacing = 1.6.sp), color = colors.textTertiary)
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
    // pad to Sunday
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
    Column(modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { cell ->
                        val level = cell?.level ?: 0
                        Box(
                            Modifier
                                .size(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colors.heatmap.getOrElse(level) { colors.heatmap.first() }),
                        )
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
                        .padding(end = 4.dp)
                        .size(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c),
                )
            }
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
    val shape = if (grouped) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
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
                Text(titleOverride, style = DiaryType.entryTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Text(entry.title, style = DiaryType.entryTitle, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            if (previewOverride != null) {
                Text(previewOverride, style = DiaryType.preview, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Text(
                    entry.transcript,
                    style = DiaryType.preview,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.forEach { TagChip(it) }
                }
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
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
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = colors.textSecondary, fontSize = 12.sp, fontFamily = com.karoslabs.deardiary.ui.theme.Inter)
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Remove $name",
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(14.dp)
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
        style = DiaryType.label,
        color = colors.textTertiary,
        modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun DiaryCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalDiaryColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 4.dp),
    ) { content() }
}
