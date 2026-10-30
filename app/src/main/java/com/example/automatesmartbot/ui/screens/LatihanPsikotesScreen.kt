package com.example.automatesmartbot.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LatihanPsikotesScreen() {
    FeatureComingSoonScreen(
        title = "Latihan Psikotes",
        description = "Latihan soal psikotes untuk persiapan seleksi kerja, dengan pembahasan yang jelas.",
        icon = Icons.Default.Psychology,
        accent = Color(0xFFF97316),
        bullets = listOf(
            "Paket latihan verbal, numerik, dan logika",
            "Pembahasan jawaban setelah latihan",
            "Skor tersimpan setelah fitur tersedia",
            "Tidak menampilkan soal atau skor palsu"
        )
    )
}

