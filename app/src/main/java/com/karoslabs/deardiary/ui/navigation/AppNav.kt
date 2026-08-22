package com.karoslabs.deardiary.ui.navigation

enum class AppTab(val route: String, val label: String) {
    Record("record", "Record"),
    Journal("journal", "Journal"),
    Search("search", "Search"),
    Settings("settings", "Settings"),
}

object Routes {
    const val ENTRY = "entry/{id}"
    fun entry(id: String) = "entry/$id"
}
