package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Revisi #8: landing Psikotes; soal, penilaian, sesi dan akses server tidak diubah. */
@Composable
internal fun PsychotestHome(
    isPro: Boolean,
    onPrivate: () -> Unit,
    onCpns: () -> Unit,
    onHistory: () -> Unit,
    onStats: () -> Unit,
    onLeaderboard: () -> Unit,
    onPro: () -> Unit
) {
    val navy = Color(0xFF142A48)
    val ocean = Color(0xFF0869B8)
    val outline = Color(0xFFD3E2ED)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFF8FCFF), Color(0xFFEAF6FD)))
        ).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", fontSize = 22.sp, color = Color(0xFF0A93C1))
                Spacer(Modifier.width(7.dp))
                Text("KarirMate", color = ocean, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.weight(1f))
                com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.LIGHT)
            }
        }
        item {
            Box(Modifier.fillMaxWidth().heightIn(min = 182.dp).background(
                Brush.horizontalGradient(listOf(Color(0xFF0754A1), Color(0xFF0879C9), Color(0xFF0BB8AF))),
                RoundedCornerShape(20.dp)
            )) {
                Row(Modifier.fillMaxWidth().padding(23.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🧠  Psikotes KarirMate", fontSize = 23.sp,
                            color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
                        Text("Latihan tes kerja, evaluasi kemampuan, dan persiapan seleksi yang lebih percaya diri.",
                            fontSize = 13.sp, color = Color.White, lineHeight = 19.sp)
                        Surface(color = Color.White.copy(alpha = .19f), shape = RoundedCornerShape(30.dp)) {
                            Text("Latihan gratis & simulasi PRO", color = Color.White,
                                fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White.copy(alpha = .9f),
                        modifier = Modifier.size(80.dp))
                }
            }
        }
        item { Text("Pilih Jenis Psikotes", color = navy, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
        item {
            PsychotestEntryCard(
                title = "Tes Kerja Swasta", subtitle = "Logika, numerik, verbal, ketelitian, dan kepribadian",
                icon = Icons.Default.Work, tint = Color(0xFF0877C5), light = Color(0xFFE5F5FF),
                onClick = onPrivate
            )
        }
        item {
            PsychotestEntryCard(
                title = "Persiapan CPNS", subtitle = "TWK, TIU, TKP, simulasi CAT, dan SKB",
                icon = Icons.Default.AccountBalance, tint = Color(0xFF0F9A81), light = Color(0xFFE4F9F3),
                onClick = onCpns
            )
        }
        item { Text("Aktivitas & Progres", color = navy, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PsychotestQuickCard("Riwayat", "Latihanmu", Icons.Default.History, Color(0xFF1575BC), Modifier.weight(1f), onHistory)
                PsychotestQuickCard("Statistik", if (isPro) "Lihat progres" else "PRO", Icons.Default.Insights, Color(0xFF0B968D), Modifier.weight(1f), onStats)
                PsychotestQuickCard("Peringkat", if (isPro) "Leaderboard" else "PRO", Icons.Default.EmojiEvents, Color(0xFFDB990C), Modifier.weight(1f), onLeaderboard)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, outline), shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✓  Latihan FREE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = navy)
                    Text("Kategori dasar • 10–20 soal per sesi • riwayat latihan",
                        color = Color(0xFF526781), fontSize = 13.sp)
                }
            }
        }
        item {
            Card(onClick = onPro, colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EA)),
                border = BorderStroke(1.dp, Color(0xFFF2D89C)), shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFD29B16), modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Simulasi & Analisis PRO", fontWeight = FontWeight.ExtraBold, color = navy)
                        Text("Simulasi lengkap, analisis mendalam, dan peringkat", color = Color(0xFF526781), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ocean)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PsychotestEntryCard(
    title: String, subtitle: String, icon: ImageVector, tint: Color, light: Color,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC7DEEC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(65.dp).background(light, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color(0xFF142A48), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Color(0xFF526781), fontSize = 12.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun PsychotestQuickCard(
    title: String, subtitle: String, icon: ImageVector, tint: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = modifier.heightIn(min = 102.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD3E2ED)), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF142A48), fontSize = 13.sp)
            Text(subtitle, color = Color(0xFF607187), fontSize = 10.sp)
        }
    }
}
