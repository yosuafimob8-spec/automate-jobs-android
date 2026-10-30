package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AutoUploadScreen(isPro: Boolean) {
    var caption by remember { mutableStateOf("Tips & Tricks Android Development & Compose #coding #android #developer") }
    var selectedPlatform by remember { mutableStateOf("TikTok") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("Bot Upload: Siap menjadwalkan video.") }

    val platforms = listOf("TikTok", "Instagram Reels", "YouTube Shorts")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FAFF))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 Auto Upload Video Bot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF17324D)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload video otomatis ke berbagai platform sosial media secara terjadwal.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Platform Selection
            Text(text = "Pilih Platform Tujuan:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1678C8))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                platforms.forEach { platform ->
                    FilterChip(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatform = platform },
                        label = { Text(platform, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color(0xFF17324D),
                            containerColor = Color(0xFFFFFFFF),
                            labelColor = Color(0xFF64748B)
                        )
                    )
                }
            }

            // Video File Picker Mock
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFF1678C8))
                        Text(text = "tutorial_compose_v1.mp4 (45 MB)", fontSize = 13.sp, color = Color(0xFF17324D))
                    }
                    TextButton(onClick = { }) {
                        Text("Ganti", color = Color(0xFF38BDF8))
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption & Hashtags") },
                shape = RoundedCornerShape(18.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFD8E9F5),
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    focusedTextColor = Color(0xFF17324D),
                    unfocusedTextColor = Color(0xFF17324D)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (!isPro) {
                        uploadStatus = "Batas Free Plan tercapai (1 video/hari). Upgrade ke PRO untuk upload tanpa batas."
                    } else {
                        isUploading = !isUploading
                        uploadStatus = if (isUploading) "Mengunggah video ke $selectedPlatform..." else "Upload dijeda."
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUploading) Color(0xFFEF4444) else Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUploading) "Batalkan Upload" else "Mulai Auto Upload ke $selectedPlatform",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Status Bot Upload:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1678C8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = uploadStatus, fontSize = 13.sp, color = Color(0xFF17324D))
                }
            }
        }
    }
}
