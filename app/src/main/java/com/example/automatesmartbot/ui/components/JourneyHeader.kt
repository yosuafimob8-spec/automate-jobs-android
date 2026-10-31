package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun JourneyHeader(title: String, subtitle: String, progress: Float? = null) {
    val colors = MaterialTheme.colorScheme
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.background(Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer))).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onPrimaryContainer)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
            progress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = colors.primary, trackColor = colors.surface) }
        }
    }
}
