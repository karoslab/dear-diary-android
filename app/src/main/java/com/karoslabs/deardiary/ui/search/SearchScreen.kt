package com.karoslabs.deardiary.ui.search

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karoslabs.deardiary.DearDiaryApp
import com.karoslabs.deardiary.domain.JournalEntry
import com.karoslabs.deardiary.ui.components.AppHeader
import com.karoslabs.deardiary.ui.components.EntryCard
import com.karoslabs.deardiary.ui.components.ScreenTitle
import com.karoslabs.deardiary.ui.theme.DiaryType
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<JournalEntry> = emptyList(),
    val ran: Boolean = false,
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as DearDiaryApp).container.repository
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        viewModelScope.launch {
            val results = if (q.isBlank()) emptyList() else repo.search(q)
            _state.update { it.copy(results = results, ran = q.isNotBlank()) }
        }
    }
}

@Composable
fun SearchScreen(
    onOpenEntry: (String) -> Unit,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = LocalDiaryColors.current
    var focusedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focusedOnce = true }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        AppHeader()
        ScreenTitle("Search")
        Spacer(Modifier.height(16.dp))
        BasicTextField(
            value = state.query,
            onValueChange = vm::onQuery,
            textStyle = DiaryType.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.gold),
            singleLine = true,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Box {
                    if (state.query.isEmpty()) {
                        Text("Search local entries", color = colors.textTertiary, style = DiaryType.body)
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "ran over local entries, 0 bytes sent",
            style = DiaryType.mono,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            if (!state.ran) "type to search this device" else "${state.results.size} ${if (state.results.size == 1) "result" else "results"}",
            style = DiaryType.mono,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
        )
        Spacer(Modifier.height(12.dp))
        if (state.results.isNotEmpty()) {
            LazyColumn(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surface),
            ) {
                itemsIndexed(state.results, key = { _, e -> e.id }) { index, entry ->
                    EntryCard(
                        entry = entry,
                        onClick = { onOpenEntry(entry.id) },
                        grouped = true,
                        titleOverride = highlightQuery(entry.title, state.query, colors.gold),
                        previewOverride = highlightQuery(entry.transcript, state.query, colors.gold),
                    )
                    if (index != state.results.lastIndex) {
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

private fun highlightQuery(text: String, query: String, underline: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val needle = query.trim().lowercase(Locale.getDefault())
    val lower = text.lowercase(Locale.getDefault())
    return buildAnnotatedString {
        append(text)
        var start = 0
        while (true) {
            val index = lower.indexOf(needle, start)
            if (index < 0) break
            addStyle(
                SpanStyle(
                    color = underline,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ),
                index,
                index + needle.length,
            )
            start = index + needle.length
        }
    }
}
