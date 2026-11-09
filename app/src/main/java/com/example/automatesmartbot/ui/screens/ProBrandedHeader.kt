package com.example.automatesmartbot.ui.screens

import androidx.compose.runtime.Composable
import com.example.automatesmartbot.ui.components.UnifiedOceanHeader

@Composable
internal fun ProBrandedHeader(onBack: () -> Unit) {
    UnifiedOceanHeader("Paket PRO", "Pilih paket premium KarirMate", onBack = onBack)
}
