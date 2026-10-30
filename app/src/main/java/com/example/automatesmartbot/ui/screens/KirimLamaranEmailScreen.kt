package com.example.automatesmartbot.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun KirimLamaranEmailScreen(
    isPro: Boolean,
    isOnline: Boolean = true,
    onNavigateProfil: () -> Unit
) {
    KirimLamaranScreen(
        isPro = isPro,
        isOnline = isOnline,
        onNavigateProfil = onNavigateProfil
    )
}
