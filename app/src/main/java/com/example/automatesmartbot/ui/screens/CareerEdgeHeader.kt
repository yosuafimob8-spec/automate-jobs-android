package com.example.automatesmartbot.ui.screens

import androidx.compose.runtime.Composable
import com.example.automatesmartbot.ui.components.UnifiedOceanHeader

@Composable
internal fun CareerEdgeHeader(
    title: String, subtitle: String, isPro: Boolean, onBack: () -> Unit, onProfile: () -> Unit
) {
    UnifiedOceanHeader(title, subtitle, onBack = onBack, onProfile = onProfile)
}
