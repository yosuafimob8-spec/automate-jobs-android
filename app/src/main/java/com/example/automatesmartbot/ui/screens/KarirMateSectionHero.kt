package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun KarirMateSectionHero(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("KARIRMATE", color = Color(0xFFFF762C), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
            Text(title, color = Color(0xFF202020), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
            Text(description, color = Color(0xFF515A63), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(color = Color(0xFFE9ECEF))
            Text("●  Siap digunakan", color = Color(0xFF00A99D), style = MaterialTheme.typography.labelMedium)
        }
    }
}
