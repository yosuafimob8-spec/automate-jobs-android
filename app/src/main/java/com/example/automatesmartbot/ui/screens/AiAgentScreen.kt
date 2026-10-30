package com.example.automatesmartbot.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AiAgentScreen() {
    FeatureComingSoonScreen(
        title = "AI Agent",
        description = "Asisten otomasi untuk membantu alur pencarian kerja, dengan kontrol dan persetujuan pengguna.",
        icon = Icons.Default.SmartToy,
        accent = Color(0xFFEC4899),
        bullets = listOf(
            "Rencana langkah yang dapat ditinjau",
            "Persetujuan sebelum tindakan dijalankan",
            "Kontrol dan batas yang jelas",
            "Tidak ada eksekusi otomatis tanpa persetujuan"
        )
    )
}

