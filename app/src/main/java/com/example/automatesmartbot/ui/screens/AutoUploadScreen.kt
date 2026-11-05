package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

@Composable
fun AutoUploadScreen(isPro: Boolean) {
    var caption by remember { mutableStateOf("Tips & Tricks Pengembangan Karier & Interview Kerja #karirmate #loker #karier") }
    var selectedPlatform by remember { mutableStateOf("TikTok") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("Bot Upload: Siap menjadwalkan video konten karier.") }

    val platforms = listOf("TikTok", "Instagram Reels", "YouTube Shorts")

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
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Auto Upload Video Bot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = KarirTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload dan jadwalkan video konten secara otomatis ke berbagai platform media sosial.",
                        fontSize = 12.5.sp,
                        color = KarirTextMuted
                    )
                }
            }

            // Platform Selection
            Text(text = "Pilih Platform Tujuan:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = KarirTextDark)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                platforms.forEach { platform ->
                    val isSelected = selectedPlatform == platform
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPlatform = platform },
                        label = { Text(platform, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5F7),
                            selectedLabelColor = Color(0xFFE65100),
                            containerColor = Color.White,
                            labelColor = KarirTextDark
                        ),
                        border = BorderStroke(1.dp, if (isSelected) KarirOrange else Color(0xFFC6DFE8))
                    )
                }
            }

            // Video File Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = KarirOrange)
                        Text(text = "video_materi_karier_v1.mp4 (45 MB)", fontSize = 12.5.sp, color = KarirTextDark, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = { }) {
                        Text("Ganti", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption & Hashtags") },
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KarirOrange,
                    unfocusedBorderColor = Color(0xFFC6DFE8),
                    cursorColor = KarirOrange,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = KarirTextDark,
                    unfocusedTextColor = KarirTextDark
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
                    containerColor = if (isUploading) Color(0xFFDC2626) else KarirOrange
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUploading) "Batalkan Upload" else "Mulai Auto Upload ke $selectedPlatform",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Status Bot Upload:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = KarirOrangeDeep)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = uploadStatus, fontSize = 12.5.sp, color = KarirTextDark)
                }
            }
        }
    }
}
