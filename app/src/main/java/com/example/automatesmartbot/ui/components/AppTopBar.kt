package com.example.automatesmartbot.ui.components

import androidx.compose.runtime.Composable

@Composable
fun AppTopBar(
    title: String,
    isPro: Boolean,
    onNavigateProfil: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    UnifiedOceanHeader(title, "KarirMate", onBack = onBack, onProfile = onNavigateProfil)
}
