package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

@Composable
fun AutoApplyPortalScreen(isPro: Boolean) {
    var selectedPortal by remember { mutableStateOf("Kalibrr") }
    var isRunning by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("Bot siap berjalan di portal terpilih.") }

    val portals = listOf("Kalibrr", "Indeed", "Glints")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = KarirOrange)
                        Text(
                            text = "Auto Apply Portal Multi-Platform",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = KarirTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Otomatisasi pengiriman lamaran kerja langsung terhubung ke profil Kalibrr, Indeed, dan Glints Anda.",
                        fontSize = 12.5.sp,
                        color = KarirTextMuted
                    )
                }
            }

            // Portal Selector Chips
            Text(text = "Pilih Platform Portal:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = KarirTextDark)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                portals.forEach { portal ->
                    val isSelected = selectedPortal == portal
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPortal = portal },
                        label = { Text(portal, fontSize = 12.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFF3E0),
                            selectedLabelColor = Color(0xFFE65100),
                            containerColor = Color.White,
                            labelColor = KarirTextDark
                        ),
                        border = BorderStroke(1.dp, if (isSelected) KarirOrange else Color(0xFFFFDEC9))
                    )
                }
            }

            // Portal Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Status Koneksi: $selectedPortal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KarirTextDark)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Text(text = "Akun terhubung • Sesi Aktif", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
                    }
                    Text(
                        text = "Target Posisi: Berdasarkan preferensi profil aktif\nResume: CV terdaftar di backend",
                        fontSize = 12.sp,
                        color = KarirTextMuted
                    )
                }
            }

            Button(
                onClick = {
                    isRunning = !isRunning
                    logText = if (isRunning) {
                        "Bot Auto Apply aktif di $selectedPortal. Memproses lamaran..."
                    } else {
                        "Bot dihentikan."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFDC2626) else KarirOrange
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Hentikan Bot $selectedPortal" else "Mulai Auto Apply di $selectedPortal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Log Eksekusi:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = KarirOrangeDeep)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = logText, fontSize = 12.5.sp, color = KarirTextDark)
                }
            }
        }
    }
}
