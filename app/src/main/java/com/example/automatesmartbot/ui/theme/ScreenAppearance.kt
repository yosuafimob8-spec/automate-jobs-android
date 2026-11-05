package com.example.automatesmartbot.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val ScreenText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

val ScreenMuted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val ScreenAccent: Color
    @Composable get() = MaterialTheme.colorScheme.primary

val ScreenSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

/** Keep the verified V78 light colors; adapt their surrounding canvas in dark mode. */
@Composable
fun screenCanvas(light: Color, dark: Color = DarkSurfaceVariant): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < .5f) dark else light

val ScreenPageGradient: Brush
    @Composable get() = if (MaterialTheme.colorScheme.surface.luminance() < .5f)
        Brush.verticalGradient(listOf(DarkBackground, DarkSurface, DarkBackground)) else KarirPageGradient

/** Use the active theme primary color for accessible buttons in both modes. */
val OnKarirOrange: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

@Composable
fun karirButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = OnKarirOrange,
    disabledContainerColor = screenCanvas(Color(0xFFE2E8F0)),
    disabledContentColor = ScreenText
)

@Composable
fun karirInputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ScreenText,
    unfocusedTextColor = ScreenText,
    disabledTextColor = ScreenMuted,
    errorTextColor = ScreenText,
    focusedContainerColor = ScreenSurface,
    unfocusedContainerColor = ScreenSurface,
    disabledContainerColor = ScreenSurface,
    errorContainerColor = ScreenSurface,
    focusedLabelColor = ScreenAccent,
    unfocusedLabelColor = ScreenMuted,
    focusedPlaceholderColor = ScreenMuted,
    unfocusedPlaceholderColor = ScreenMuted,
    focusedLeadingIconColor = ScreenAccent,
    unfocusedLeadingIconColor = ScreenMuted,
    focusedTrailingIconColor = ScreenAccent,
    unfocusedTrailingIconColor = ScreenMuted,
    cursorColor = ScreenAccent,
    errorCursorColor = MaterialTheme.colorScheme.error,
    focusedBorderColor = ScreenAccent,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error
)
