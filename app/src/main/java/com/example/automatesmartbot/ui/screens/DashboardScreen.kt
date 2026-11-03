package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.model.DashboardStatusResponse
import com.example.automatesmartbot.ui.navigation.Screen
import com.example.automatesmartbot.ui.theme.CardBorder
import com.example.automatesmartbot.ui.theme.DarkBackground
import com.example.automatesmartbot.ui.theme.DarkSurface
import com.example.automatesmartbot.ui.theme.DarkSurfaceVariant
import com.example.automatesmartbot.ui.theme.FreeCyan
import com.example.automatesmartbot.ui.theme.LightBlueAccent
import com.example.automatesmartbot.ui.theme.PrimaryBlue
import com.example.automatesmartbot.ui.theme.ProGold
import com.example.automatesmartbot.ui.theme.TextGray
import com.example.automatesmartbot.ui.theme.TextWhite
import com.example.automatesmartbot.ui.viewmodel.DashboardViewModel

private data class DashboardFeature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String,
    val statusLabel: String? = null
)

private data class StatItem(
    val title: String,
    val value: String,
    val caption: String,
    val accent: Color
)

private val jobFeatures = listOf(
    DashboardFeature(
        title = "Pelatihan Kerja",
        subtitle = "Belajar mandiri",
        icon = Icons.Default.Work,
        accent = PrimaryBlue,
        route = Screen.JobTraining.route
    ),
    DashboardFeature(
        title = "Kirim Lamaran",
        subtitle = "Gmail, persetujuan & antrean lamaran",
        icon = Icons.AutoMirrored.Filled.Send,
        accent = Color(0xFF22C55E),
        route = Screen.KirimLamaran.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "CV Builder",
        subtitle = "Susun CV dari data profil",
        icon = Icons.Default.Description,
        accent = Color(0xFF8B5CF6),
        route = Screen.CvBuilder.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "Latihan Psikotes",
        subtitle = "Persiapan tes kerja",
        icon = Icons.Default.Psychology,
        accent = Color(0xFFF97316),
        route = Screen.LatihanPsikotes.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "Workpedia",
        subtitle = "Istilah dan hak pekerja",
        icon = Icons.Default.Description,
        accent = Color(0xFF06B6D4),
        route = Screen.WorkPedia.route
    )
)

private val interviewFeatures = listOf(
    DashboardFeature(
        title = "AI Interview",
        subtitle = "Latihan wawancara teks dan suara",
        icon = Icons.Default.Mic,
        accent = Color(0xFF06B6D4),
        route = Screen.AiInterview.route
    )
)

private val automationFeatures = listOf(
    DashboardFeature(
        title = "Kalkulator Gaji & Lembur",
        subtitle = "Lembur, BPJS, PPh 21 & THR",
        icon = Icons.Default.History,
        accent = Color(0xFF6366F1),
        route = Screen.SalaryCalculator.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "Surat Lamaran & Resign",
        subtitle = "Editor surat profesional & PDF",
        icon = Icons.Default.FolderShared,
        accent = Color(0xFF14B8A6),
        route = Screen.CareerLetters.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "Dokumen Karier",
        subtitle = "Gabung, susun & putar PDF",
        icon = Icons.Default.Person,
        accent = Color(0xFFEAB308),
        route = Screen.CareerDocuments.route
    )
)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel
) {
    val status by viewModel.status.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var search by remember { mutableStateOf("") }
    val brand = Color(0xFFC54814)
    val teal = Color(0xFF00796F)
    val dark = Color(0xFF252525)
    val menu = listOf(
        Triple("Pelatihan\nKerja", Icons.Default.Work, Screen.JobTraining.route),
        Triple("Kalkulator Gaji\n& Lembur", Icons.Default.History, Screen.SalaryCalculator.route),
        Triple("Surat Izin &\nDokumen Kerja", Icons.Default.Description, Screen.SuratIzinDokumen.route),
        Triple("Workpedia", Icons.Default.Psychology, Screen.WorkPedia.route),
        Triple("CV Builder", Icons.Default.FolderShared, Screen.CvBuilder.route),
        Triple("Surat Lamaran\nOtomatis", Icons.Default.Email, Screen.CareerLetters.route),
        Triple("AI Interview", Icons.Default.Mic, Screen.AiInterview.route),
        Triple("CV Analyze", Icons.Default.Description, Screen.CvBuilder.route)
    )
    val colors = listOf(brand, teal, Color(0xFF8B5CF6), Color(0xFFDAA520), Color(0xFF2563EB), brand, teal, Color(0xFF7C3AED))
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA)),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF171717), dark, Color(0xFF444444)))
                ).padding(horizontal = 20.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("KarirMate", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(40.dp), color = Color(0xFFFF6B00)) {
                        Text(if (status?.isPro == true) "✦ PRO" else "FREE", color = Color.White, modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.Person, "Profil", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onNavigate(Screen.Profil.route) })
                }
                Text("Halo, " + (status?.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(if (status?.isOnline == true) "● Online  •  Siapkan karier impianmu" else "○ Offline  •  Siapkan karier impianmu", color = Color(0xFFE0E0E0), fontSize = 12.sp)
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text("Cari fitur KarirMate", color = Color(0xFF777777)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = brand, unfocusedBorderColor = Color.Transparent)
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
                border = BorderStroke(1.dp, Color(0xFFE8E8E8))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Work, null, tint = brand)
                        Spacer(Modifier.width(8.dp))
                        Text("Statistik Ketenagakerjaan Indonesia", fontWeight = FontWeight.Bold, color = dark, fontSize = 14.sp)
                    }
                    Text("Sumber: Badan Pusat Statistik (BPS)", color = teal, fontSize = 11.sp)
                    Text("Angka resmi belum tersedia • Tidak menampilkan data perkiraan", fontSize = 10.sp, color = Color(0xFF666666))
                    listOf("Pengangguran", "Kemiskinan", "Angkatan kerja", "Penduduk bekerja", "Rata-rata upah").chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            row.forEachIndexed { index, label ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(13.dp),
                                    color = if (index == 0) Color(0xFFFFF2E8) else Color(0xFFE5F5F1)
                                ) {
                                    Column(Modifier.padding(7.dp)) {
                                        Text(label, fontSize = 10.sp, color = dark)
                                        Spacer(Modifier.height(2.dp))
                                        Text("Belum tersedia", fontSize = 9.sp, color = Color(0xFF666666), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Jelajahi Fitur", fontSize = 19.sp, color = dark, fontWeight = FontWeight.ExtraBold)
                menu.filter { search.isBlank() || it.first.replace("\n", " ").contains(search, ignoreCase = true) }.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { entry ->
                            val idx = menu.indexOf(entry)
                            val accent = colors[idx]
                            Card(
                                onClick = { onNavigate(entry.third) },
                                modifier = Modifier.weight(1f).height(116.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, accent.copy(alpha = .24f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(listOf(Color.White, accent.copy(alpha = .11f)))
                                    ).padding(horizontal = 3.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(shape = CircleShape, color = accent.copy(alpha = .15f)) {
                                        Icon(entry.second, null, tint = accent, modifier = Modifier.padding(10.dp).size(24.dp))
                                    }
                                    Text(entry.first, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = dark, maxLines = 3, lineHeight = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(containerColor = dark),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = .65f))
            ) {
                Column(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF171717), dark, Color(0xFF444444)))).padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("✦ Syarat Sertifikat KarirMate", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF2E8), border = BorderStroke(2.dp, Color(0xFFFF6B00))) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.WorkspacePremium, null, tint = brand, modifier = Modifier.size(44.dp))
                                Text("SERTIFIKAT", fontSize = 9.sp, color = dark, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("PRO aktif", "Pelatihan kerja lulus ≥80", "AI Interview lulus ≥80", "Psikotes campuran lulus ≥80").forEach {
                                Text("○  $it", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    Text("Status persyaratan belum dapat diverifikasi server", color = Color(0xFFFFC18F), fontSize = 12.sp)
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF7B7B7B)),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("Buat Sertifikat") }
                }
            }
        }
        if (isLoading && status == null) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
        }
    }
}

@Composable
private fun ConnectionPill(isOnline: Boolean) {
    Surface(
        color = if (isOnline) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = if (isOnline) Color(0xFF34D399) else Color(0xFFF87171),
                        shape = CircleShape
                    )
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color(0xFF222222)
            )
        }
    }
}

@Composable
private fun WelcomeCard(
    status: DashboardStatusResponse?,
    onCompleteProfile: () -> Unit
) {
    val isPro = status?.isPro == true
    val displayName = status?.displayName?.takeIf { it.isNotBlank() } ?: "SmartBot User"
    val profileText = when (status?.profileCompleted) {
        true -> "Profil lengkap"
        false -> "Profil belum lengkap"
        null -> "Status profil belum diketahui"
    }
    val emailText = when (status?.emailVerified) {
        true -> "Email terverifikasi"
        false -> "Email belum terverifikasi"
        null -> "Status email belum diketahui"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Halo, $displayName",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        ConnectionPill(isOnline = status?.isOnline == true)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AutoMate Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF222222)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                MembershipBadge(isPro = isPro)
            }

            HorizontalDivider(color = Color(0xFFE2EAF5), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusPill(
                    modifier = Modifier.weight(1f),
                    icon = if (status?.profileCompleted == true) Icons.Default.CheckCircle else Icons.Default.Person,
                    text = profileText,
                    tint = if (status?.profileCompleted == true) Color(0xFF34D399) else Color(0xFFFBBF24)
                )
                StatusPill(
                    modifier = Modifier.weight(1f),
                    icon = if (status?.emailVerified == true) Icons.Default.CheckCircle else Icons.Default.Email,
                    text = emailText,
                    tint = if (status?.emailVerified == true) Color(0xFF34D399) else Color(0xFFFBBF24)
                )
            }

            if (status?.profileCompleted == false) {
                Button(
                    onClick = onCompleteProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Lengkapi Profil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun MembershipBadge(isPro: Boolean) {
    Surface(
        color = if (isPro) ProGold else FreeCyan,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isPro) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = if (isPro) "PRO" else "FREE",
                color = Color(0xFF222222),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatusPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                color = Color(0xFF222222),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AccountStats(status: DashboardStatusResponse?) {
    val stats = listOf(
        StatItem(
            title = "Lowongan",
            value = status?.jobCount?.toString() ?: "-",
            caption = "Posisi & lokasi pilihan",
            accent = PrimaryBlue
        ),
        StatItem(
            title = "Profil",
            value = when (status?.profileCompleted) {
                true -> "Lengkap"
                false -> "Belum"
                null -> "-"
            },
            caption = "Kelengkapan data profil",
            accent = Color(0xFF22C55E)
        ),
        StatItem(
            title = "Email",
            value = when (status?.emailVerified) {
                true -> "Terverifikasi"
                false -> "Belum"
                null -> "-"
            },
            caption = "Status akun",
            accent = Color(0xFFFBBF24)
        ),
        StatItem(
            title = "Member",
            value = if (status?.isPro == true) "PRO" else "FREE",
            caption = if (status?.membershipVerified == true) {
                "Status keanggotaan aktif"
            } else {
                "Status keanggotaan"
            },
            accent = ProGold
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    StatCard(item = item, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(item: StatItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(88.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2EAF5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.value,
                fontSize = 19.sp,
                color = item.accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.caption,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, itemCount: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF222222)
        )
        if (itemCount != null) {
            Text(
                text = "$itemCount fitur",
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun FeatureGrid(
    features: List<DashboardFeature>,
    onNavigate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        features.chunked(2).forEach { row ->
            if (row.size == 1) {
                val feature = row.first()
                FeatureCard(
                    feature = feature,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate(feature.route) }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { feature ->
                        FeatureCard(
                            feature = feature,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(feature.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: DashboardFeature,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(142.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = feature.accent.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, feature.accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(feature.accent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = feature.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                feature.statusLabel?.let { label ->
                    Surface(
                        color = Color(0xFFEAF3FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            fontSize = 8.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = feature.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF222222),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = feature.subtitle,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = feature.accent.copy(alpha = 0.8f),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityUnavailableCard(onOpenHistory: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = LightBlueAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Belum ada riwayat lamaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF222222)
                )
            }
            Text(
                text = "Riwayat lamaran akan tampil di sini setelah fitur pengiriman tersedia. Aplikasi tidak menampilkan status palsu.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            TextButton(
                onClick = onOpenHistory,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Buka Riwayat Lamaran", color = PrimaryBlue, fontSize = 12.sp)
            }
        }
    }
}

