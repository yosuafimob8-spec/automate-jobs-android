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
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

@Composable
internal fun KarirMateSectionHero(title: String, description: String) {
    var detailsExpanded by remember { mutableStateOf(false) }
    val accentAlpha by animateFloatAsState(if (detailsExpanded) 1f else 0.85f, label = "heroAccent")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("KARIRMATE", color = KarirOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFDEC9))
                ) {
                    Text(
                        "PUSAT KARIER",
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
            }
            Text(
                title,
                color = KarirTextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                description,
                color = KarirTextMuted,
                fontSize = 12.5.sp
            )
            HorizontalDivider(color = Color(0xFFF1E5DC))
            Row(Modifier.fillMaxWidth().clickable { detailsExpanded = !detailsExpanded }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Kembangkan kariermu selangkah demi selangkah",
                    modifier = Modifier.weight(1f).alpha(accentAlpha),
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp
                )
                Text(
                    if (detailsExpanded) "Tutup ↑" else "Info ↓",
                    color = KarirOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }
            AnimatedVisibility(visible = detailsExpanded) {
                Text(
                    "Gunakan menu di halaman ini untuk mengakses fitur yang tersedia. Hasil dan data mengikuti informasi yang benar-benar tersimpan.",
                    color = KarirTextMuted,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}
