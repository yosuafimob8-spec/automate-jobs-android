package com.example.automatesmartbot.ui.screens

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

@Composable
fun AutoApplyPortalScreen(isPro: Boolean) {
    var selectedPortal by remember { mutableStateOf("Kalibrr") }
    var isRunning by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("Bot siap berjalan di portal terpilih.") }

    val portals = listOf("Kalibrr", "Indeed", "Glints")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FAFC))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = Color(0xFF55C6A9))
                        Text(
                            text = "Auto Apply Portal Multi-Platform",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF243547)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Otomatisasi lamaran kerja langsung terhubung ke profil Kalibrr, Indeed, dan Glints.",
                        fontSize = 13.sp,
                        color = Color(0xFF52677A)
                    )
                }
            }

            // Portal Selector Chips
            Text(text = "Pilih Platform Portal:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563A6))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                portals.forEach { portal ->
                    FilterChip(
                        selected = selectedPortal == portal,
                        onClick = { selectedPortal = portal },
                        label = { Text(portal, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF55C6A9),
                            selectedLabelColor = Color(0xFF243547),
                            containerColor = Color(0xFFFFFFFF),
                            labelColor = Color(0xFF52677A)
                        )
                    )
                }
            }

            // Portal Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Status Koneksi: $selectedPortal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF243547))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                        Text(text = "Akun terhubung • Sesi Aktif", fontSize = 13.sp, color = Color(0xFF34D399))
                    }
                    Text(
                        text = "Target Posisi: Android Engineer / Kotlin Developer\nResume: CV_Andri_2025.pdf",
                        fontSize = 12.sp,
                        color = Color(0xFF52677A)
                    )
                }
            }

            Button(
                onClick = {
                    isRunning = !isRunning
                    logText = if (isRunning) {
                        "Bot Auto Apply aktif di $selectedPortal. Mengirim lamaran..."
                    } else {
                        "Bot dihentikan."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF55C6A9)
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Hentikan Bot $selectedPortal" else "Mulai Auto Apply di $selectedPortal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Log Eksekusi:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF55C6A9))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = logText, fontSize = 13.sp, color = Color(0xFF243547))
                }
            }
        }
    }
}
