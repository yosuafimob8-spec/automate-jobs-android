package com.example.automatesmartbot.ui.screens

import androidx.compose.runtime.Composable
import com.example.automatesmartbot.ui.components.UnifiedOceanHeader

@Composable
internal fun KirimLamaranBrandedHeader(isPro: Boolean, onBack: () -> Unit) {
    UnifiedOceanHeader("Kirim Lamaran", "Kelola dan kirim lamaran kerja dengan mudah", onBack = onBack)
}
