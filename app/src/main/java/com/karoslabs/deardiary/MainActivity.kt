package com.karoslabs.deardiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.karoslabs.deardiary.ui.components.FloatingTabBar
import com.karoslabs.deardiary.ui.entry.EntryDetailScreen
import com.karoslabs.deardiary.ui.journal.JournalScreen
import com.karoslabs.deardiary.ui.navigation.AppTab
import com.karoslabs.deardiary.ui.navigation.Routes
import com.karoslabs.deardiary.ui.record.RecordScreen
import com.karoslabs.deardiary.ui.search.SearchScreen
import com.karoslabs.deardiary.ui.settings.SettingsScreen
import com.karoslabs.deardiary.ui.theme.DearDiaryTheme
import com.karoslabs.deardiary.ui.theme.LocalDiaryColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = (application as DearDiaryApp).container.userPrefs
        setContent {
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(
                com.karoslabs.deardiary.domain.ThemeMode.System,
            )
            DearDiaryTheme(themeMode) {
                DearDiaryRoot()
            }
        }
    }
}

@Composable
fun DearDiaryRoot() {
    val colors = LocalDiaryColors.current
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(AppTab.Record.name) }
    val selected = AppTab.valueOf(tab)

    fun goTab(next: AppTab) {
        tab = next.name
        nav.navigate(next.route) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openEntry(id: String) {
        nav.navigate(Routes.entry(id))
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        NavHost(
            navController = nav,
            startDestination = AppTab.Record.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppTab.Record.route) {
                RecordScreen(onOpenEntry = ::openEntry)
            }
            composable(AppTab.Journal.route) {
                JournalScreen(onOpenEntry = ::openEntry)
            }
            composable(AppTab.Search.route) {
                SearchScreen(onOpenEntry = ::openEntry)
            }
            composable(AppTab.Settings.route) {
                SettingsScreen()
            }
            composable(
                Routes.ENTRY,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { backStack ->
                val id = backStack.arguments?.getString("id").orEmpty()
                EntryDetailScreen(entryId = id, onBack = { nav.popBackStack() })
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            FloatingTabBar(selected = selected, onSelect = ::goTab)
        }
    }
}
