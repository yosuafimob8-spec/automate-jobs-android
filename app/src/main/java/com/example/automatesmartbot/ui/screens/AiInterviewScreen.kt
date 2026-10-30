package com.example.automatesmartbot.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AiInterviewScreen() {
    FeatureComingSoonScreen(
        title = "AI Interview",
        description = "Latih jawaban wawancara kerja dengan pertanyaan yang relevan dan umpan balik terarah.",
        icon = Icons.Default.Mic,
        accent = Color(0xFF06B6D4),
        bullets = listOf(
            "Pertanyaan wawancara sesuai posisi",
            "Latihan jawaban secara bertahap",
            "Umpan balik setelah sesi",
            "Sesi AI aktif setelah fitur tersedia"
        )
    )
}

