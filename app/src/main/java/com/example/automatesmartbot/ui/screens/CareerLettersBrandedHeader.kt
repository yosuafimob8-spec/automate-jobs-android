package com.example.automatesmartbot.ui.screens

import androidx.compose.runtime.Composable
import com.example.automatesmartbot.ui.components.UnifiedOceanHeader

@Composable
internal fun CareerLettersBrandedHeader(isPro: Boolean, onBack: () -> Unit) {
    UnifiedOceanHeader("Surat Lamaran & Resign", "Kelola surat lamaran dan dokumen karier", onBack = onBack)
}
