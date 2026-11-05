package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

@Composable
fun RiwayatScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KarirMateSectionHero("Riwayat Lamaran", "Pantau pengiriman yang benar-benar tercatat, tanpa status palsu.")
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, tint = KarirOrange)
                        Text(text = "Status Pengiriman", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                    }
                    Text(
                        text = "Pantau status lamaran kerja yang sudah Anda ajukan atau kirimkan.",
                        fontSize = 12.sp,
                        color = KarirTextMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFFFB280),
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = "Belum ada riwayat lamaran",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarirTextDark
                    )
                    Text(
                        text = "Riwayat resmi akan otomatis muncul setelah pengiriman email atau lamaran diproses.",
                        fontSize = 12.sp,
                        color = KarirTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}
