package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        subtitle = "Belajar mandiri & sertifikat latihan",
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
        accent = Color(0xFF55C6A9),
        route = Screen.CvBuilder.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "Latihan Psikotes",
        subtitle = "Persiapan tes kerja",
        icon = Icons.Default.Psychology,
        accent = Color(0xFF2563A6),
        route = Screen.LatihanPsikotes.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "AI Interview",
        subtitle = "Latihan wawancara kerja",
        icon = Icons.Default.Mic,
        accent = Color(0xFF06B6D4),
        route = Screen.AiInterview.route,
        statusLabel = "Segera Hadir"
    )
)

private val automationFeatures = listOf(
    DashboardFeature(
        title = "AI Agent",
        subtitle = "Otomasi dengan persetujuan Anda",
        icon = Icons.Default.SmartToy,
        accent = Color(0xFF55C6A9),
        route = Screen.AiAgent.route,
        statusLabel = "Segera Hadir"
    ),
    DashboardFeature(
        title = "Kalkulator Gaji",
        subtitle = "Lembur, BPJS, PPh 21 & THR",
        icon = Icons.Default.History,
        accent = Color(0xFF2563A6),
        route = Screen.SalaryCalculator.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "Surat Lamaran & Resign",
        subtitle = "Editor surat profesional & PDF",
        icon = Icons.Default.FolderShared,
        accent = Color(0xFF55C6A9),
        route = Screen.CareerLetters.route,
        statusLabel = null
    ),
    DashboardFeature(
        title = "Dokumen Karier",
        subtitle = "Gabung, susun & putar PDF",
        icon = Icons.Default.Person,
        accent = Color(0xFFF2C75C),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FAFC))
    ) {
        if (isLoading && status == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "KARIRMATE  /  CAREER HUB",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFFB8F1E3)
                            )
                            Text(
                                text = "Bangun karier\nlebih percaya diri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 25.sp,
                                lineHeight = 30.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Persiapkan CV, latihan, dan lamaran dalam satu tempat.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFFE4F2FC)
                            )
                        }
                    }
                }
                item {
                    WelcomeCard(
                        status = status,
                        onCompleteProfile = {
                            onNavigate(Screen.OnboardingProfile.route)
                        }
                    )
                }
                item { CareerJourneyCard(status = status, onCompleteProfile = { onNavigate(Screen.OnboardingProfile.route) }) }
                item { CareerBillboardPlaceholder() }
                item { AccountStats(status = status) }
                item { SectionHeader(title = "Cari Kerja & Persiapan", itemCount = jobFeatures.size) }
                item { FeatureGrid(features = jobFeatures, onNavigate = onNavigate) }
                item { SectionHeader(title = "Otomasi & Dokumen", itemCount = automationFeatures.size) }
                item { FeatureGrid(features = automationFeatures, onNavigate = onNavigate) }
                item { SectionHeader(title = "Aktivitas Terakhir") }
                item { ActivityUnavailableCard(onOpenHistory = { onNavigate(Screen.Riwayat.route) }) }
            }
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
                color = Color(0xFF243547)
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
    val displayName = status?.displayName?.takeIf { it.isNotBlank() } ?: "Sahabat Karir"
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
                            color = Color(0xFF52677A)
                        )
                        ConnectionPill(isOnline = status?.isOnline == true)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Perjalanan Kariermu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF243547)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                MembershipBadge(isPro = isPro)
            }

            HorizontalDivider(color = Color(0xFFD9E9ED), thickness = 1.dp)

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
                color = Color(0xFF102B49),
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
        color = Color(0xFFF5FAFC),
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
                color = Color(0xFF243547),
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
        border = BorderStroke(1.dp, Color(0xFFD9E9ED))
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
                color = Color(0xFF52677A),
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
                color = Color(0xFF52677A),
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
            color = Color(0xFF243547)
        )
        if (itemCount != null) {
            Text(
                text = "$itemCount fitur",
                fontSize = 10.sp,
                color = Color(0xFF52677A)
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
            .height(156.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFD8E9EE))
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
                        .background(feature.accent.copy(alpha = 0.13f), RoundedCornerShape(13.dp)),
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
                            color = Color(0xFF52677A),
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
                    color = Color(0xFF243547),
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
                        color = Color(0xFF52677A),
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
                    color = Color(0xFF243547)
                )
            }
            Text(
                text = "Riwayat lamaran akan tampil di sini setelah fitur pengiriman tersedia. Aplikasi tidak menampilkan status palsu.",
                fontSize = 12.sp,
                color = Color(0xFF52677A)
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


/** Career Journey displays only confirmed account progress, never fabricated achievements. */
@Composable
private fun CareerJourneyCard(
    status: DashboardStatusResponse?,
    onCompleteProfile: () -> Unit
) {
    val completed = status?.profileCompleted == true
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5F7)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Career Journey", color = Color(0xFF243547), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                if (completed) "Profil siap. Jelajahi pelatihan dan peluang karier."
                else "Mulai perjalanan karier dengan melengkapi profil.",
                color = Color(0xFF52677A),
                fontSize = 13.sp
            )
            LinearProgressIndicator(
                progress = { if (completed) 1f else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF55C6A9),
                trackColor = Color.White
            )
            Text(
                if (completed) "Profil: lengkap" else "Profil: belum lengkap atau belum terverifikasi",
                fontSize = 12.sp,
                color = Color(0xFF243547)
            )
            if (!completed) {
                TextButton(onClick = onCompleteProfile) {
                    Text("Lengkapi Profil", color = Color(0xFF2563A6), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Reserved ad area: no SDK, no live inventory, and no misleading sponsored content. */
@Composable
private fun CareerBillboardPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF0F8F9),
        border = BorderStroke(1.dp, Color(0xFFD1E6E9))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Career Billboard", color = Color(0xFF243547), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "Ruang informasi mitra karier — belum ada iklan.",
                color = Color(0xFF52677A),
                fontSize = 12.sp
            )
        }
    }
}
