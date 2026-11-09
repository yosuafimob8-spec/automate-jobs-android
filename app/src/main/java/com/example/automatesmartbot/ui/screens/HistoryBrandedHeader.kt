package com.example.automatesmartbot.ui.screens

import androidx.compose.runtime.Composable
import com.example.automatesmartbot.ui.components.UnifiedOceanHeader

@Composable
internal fun HistoryBrandedHeader(onBack: () -> Unit) {
    UnifiedOceanHeader("Riwayat Lamaran", "Lihat dan kelola riwayat pengiriman lamaran", onBack = onBack)
}
