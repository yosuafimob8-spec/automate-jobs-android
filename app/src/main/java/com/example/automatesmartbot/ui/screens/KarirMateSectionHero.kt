package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun KarirMateSectionHero(title: String, description: String) {
    var detailsExpanded by remember { mutableStateOf(false) }
    val accentAlpha by animateFloatAsState(if (detailsExpanded) 1f else 0.8f, label = "heroAccent")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("KARIRMATE", color = Color(0xFFC54814), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF2E8), border = BorderStroke(1.dp, Color(0xFFFFDDC5))) {
                    Text("PUSAT KARIER", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color(0xFF994010), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(title, color = Color(0xFF202020), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
            Text(description, color = Color(0xFF515A63), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(color = Color(0xFFE9ECEF))
            Row(Modifier.fillMaxWidth().clickable { detailsExpanded = !detailsExpanded }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kembangkan kariermu selangkah demi selangkah", modifier = Modifier.weight(1f).alpha(accentAlpha), color = Color(0xFF00796F), style = MaterialTheme.typography.labelMedium)
                Text(if (detailsExpanded) "Tutup ↑" else "Info ↓", color = Color(0xFFC54814), style = MaterialTheme.typography.labelMedium)
            }
            AnimatedVisibility(visible = detailsExpanded) {
                Text("Gunakan menu di halaman ini untuk mengakses fitur yang tersedia. Hasil dan data mengikuti informasi yang benar-benar tersimpan.", color = Color(0xFF515A63), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
