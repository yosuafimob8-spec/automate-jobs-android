package com.example.automatesmartbot.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CvBuilderScreen() {
    FeatureComingSoonScreen(
        title = "CV Builder",
        description = "Susun CV kerja yang rapi dan terstruktur dari data profil Anda.",
        icon = Icons.Default.Description,
        accent = Color(0xFF8B5CF6),
        bullets = listOf(
            "Template CV yang ramah sistem ATS",
            "Impor data dari profil",
            "Pratinjau sebelum diunduh",
            "Unduh PDF setelah fitur tersedia"
        )
    )
}

