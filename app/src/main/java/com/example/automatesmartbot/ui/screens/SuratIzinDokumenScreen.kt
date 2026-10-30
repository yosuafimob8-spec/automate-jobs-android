package com.example.automatesmartbot.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SuratIzinDokumenScreen() {
    FeatureComingSoonScreen(
        title = "Surat Izin & Dokumen Kerja",
        description = "Siapkan surat izin dan dokumen kerja yang sering dibutuhkan saat melamar.",
        icon = Icons.Default.FolderShared,
        accent = Color(0xFF14B8A6),
        bullets = listOf(
            "Template surat izin kerja",
            "Dokumen pendukung lamaran",
            "Isi otomatis dari profil setelah tersedia",
            "Unduh dokumen setelah fitur tersedia"
        )
    )
}

