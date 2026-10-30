package com.example.automatesmartbot.ui.screens

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

@Composable
fun RiwayatScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF60A5FA))
                        Text(text = "Riwayat Lamaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                    Text(
                        text = "Pantau status lamaran yang Anda kirim.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
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
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Belum ada riwayat lamaran",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Riwayat akan muncul setelah fitur pengiriman lamaran tersedia. Aplikasi tidak menampilkan status palsu.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

