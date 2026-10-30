package com.example.automatesmartbot.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    AUTO("Ikuti sistem"),
    LIGHT("Terang"),
    DARK("Gelap")
}

class ThemePreference(context: Context) {
    private val preferences = context.getSharedPreferences("karir_mate_preferences", Context.MODE_PRIVATE)

    var mode by mutableStateOf(
        runCatching { ThemeMode.valueOf(preferences.getString("theme_mode", ThemeMode.AUTO.name)!!) }
            .getOrDefault(ThemeMode.AUTO)
    )
        private set

    fun selectMode(value: ThemeMode) {
        mode = value
        preferences.edit().putString("theme_mode", value.name).apply()
    }
}
