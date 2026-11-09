package com.example.automatesmartbot.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.ScreenText
import com.example.automatesmartbot.ui.theme.ScreenMuted
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.ScreenPageGradient
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.data.model.DashboardStatusResponse
import com.example.automatesmartbot.ui.navigation.Screen
import com.example.automatesmartbot.ui.components.KarirMateTestBanner
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.viewmodel.DashboardViewModel

private data class MainMenuItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val iconTint: Color,
    val bgTint: Color,
    val route: String
)

private val primaryMenuItems = listOf(
    MainMenuItem(
        title = "Pelatihan\nKerja",
        subtitle = null,
        icon = Icons.Default.School,
        iconTint = Color(0xFF2563A6),
        bgTint = Color(0xFFE8F5F7),
        route = Screen.JobTraining.route
    ),
    MainMenuItem(
        title = "Kalkulator\nGaji & Lembur",
        subtitle = null,
        icon = Icons.Default.Calculate,
        iconTint = Color(0xFF16A34A),
        bgTint = Color(0xFFDCFCE7),
        route = Screen.SalaryCalculator.route
    ),
    MainMenuItem(
        title = "Dokumen\nKarier",
        subtitle = null,
        icon = Icons.Default.Article,
        iconTint = Color(0xFFD97706),
        bgTint = Color(0xFFFEF3C7),
        route = Screen.CareerDocuments.route
    ),
    MainMenuItem(
        title = "Workpedia",
        subtitle = "Info & Istilah Kerja",
        icon = Icons.Default.Layers,
        iconTint = Color(0xFF0284C7),
        bgTint = Color(0xFFE0F2FE),
        route = Screen.WorkPedia.route
    ),
    MainMenuItem(
        title = "CV Builder",
        subtitle = "Buat CV Profesional",
        icon = Icons.Default.Badge,
        iconTint = Color(0xFF2563EB),
        bgTint = Color(0xFFDBEAFE),
        route = Screen.CvBuilder.route
    ),
    MainMenuItem(
        title = "Surat Lamaran",
        subtitle = "Otomatis (AI)",
        icon = Icons.Default.Email,
        iconTint = Color(0xFFDB2777),
        bgTint = Color(0xFFFCE7F3),
        route = Screen.CareerLetters.route
    ),
    MainMenuItem(
        title = "AI Interview",
        subtitle = "Latihan Wawancara",
        icon = Icons.Default.Headphones,
        iconTint = Color(0xFF9333EA),
        bgTint = Color(0xFFF3E8FF),
        route = Screen.AiInterview.route
    ),
    MainMenuItem(
        title = "Latihan\nPsikotes",
        subtitle = null,
        icon = Icons.Default.Psychology,
        iconTint = Color(0xFFE11D48),
        bgTint = Color(0xFFFFF1F2),
        route = Screen.LatihanPsikotes.route
    )
)


@Composable
private fun DashboardProfilePhoto() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(
            "karirmate_profile_photo",
            Context.MODE_PRIVATE
        )
    }

    var photoUri by remember {
        mutableStateOf(prefs.getString("uri", null))
    }

    DisposableEffect(prefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "uri") {
                    photoUri = prefs.getString("uri", null)
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val bitmap = remember(photoUri) {
        photoUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(
                    Uri.parse(uri)
                )?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0xFFE8F5F7)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto profil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profil",
                tint = Color(0xFF2563A6),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel,
    showTestAd: Boolean = false
) {
    val status by viewModel.status.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPro = status?.isPro == true && status?.membershipVerified == true
    val displayName = status?.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna"
    val ocean = Color(0xFF2563A6)
    val mint = Color(0xFF55C6A9)
    val background = ScreenSurface
    val menu = listOf(
        MainMenuItem("CV Builder", icon = Icons.Default.Badge, iconTint = Color(0xFF16A085), bgTint = Color(0xFFE1F7F1), route = Screen.CvBuilder.route),
        MainMenuItem("Psikotes", icon = Icons.Default.Psychology, iconTint = Color(0xFF8054D7), bgTint = Color(0xFFF0E9FF), route = Screen.LatihanPsikotes.route),
        MainMenuItem("AI Interview", icon = Icons.Default.Headphones, iconTint = ocean, bgTint = Color(0xFFE4F1FF), route = Screen.AiInterview.route),
        MainMenuItem("Pelatihan", icon = Icons.Default.School, iconTint = Color(0xFFDA9B13), bgTint = Color(0xFFFFF3D3), route = Screen.JobTraining.route),
        MainMenuItem("Kalkulator Gaji", icon = Icons.Default.Calculate, iconTint = Color(0xFFF0A91B), bgTint = Color(0xFFFFF4DB), route = Screen.SalaryCalculator.route),
        MainMenuItem("Surat & Dokumen", icon = Icons.Default.Description, iconTint = Color(0xFF16A085), bgTint = Color(0xFFE1F7F1), route = Screen.CareerLetters.route),
        MainMenuItem("Workpedia", icon = Icons.Default.Work, iconTint = Color(0xFFEAA332), bgTint = Color(0xFFFFF0D9), route = Screen.WorkPedia.route),
        MainMenuItem("Sertifikat", icon = Icons.Default.WorkspacePremium, iconTint = Color(0xFFBA8A16), bgTint = Color(0xFFFFF3D3), route = Screen.Certificates.route)
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().background(background).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.automatesmartbot.ui.components.BrandLogo(modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(9.dp))
                com.example.automatesmartbot.ui.components.BrandWordmark(modifier = Modifier.weight(1f), fontSize = 22.sp)
                IconButton(onClick = { onNavigate(Screen.Riwayat.route) }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Riwayat aktivitas", tint = ScreenText)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardProfilePhoto()
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Halo, ${displayName.substringBefore(' ')} 👋", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScreenText)
                    Text("Siap lanjutkan perjalanan kariermu?", fontSize = 12.sp, color = ScreenMuted)
                }
            }
        }
        item {
            val stages = listOf(
                Triple("01", "Discover Yourself", Screen.OnboardingProfile.route),
                Triple("02", "Build Your Skills", Screen.JobTraining.route),
                Triple("03", "Prepare Yourself", Screen.CvBuilder.route),
                Triple("04", "Manage Applications", Screen.KirimLamaran.route),
                Triple("05", "Grow Your Career", Screen.SalaryCalculator.route)
            )
            // Only verified profile completion is available in DashboardStatusResponse.
            // Never show mock progress for training, interviews or applications.
            val profileVerified = status?.profileCompleted == true
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ocean)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Career Journey", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
                            Text("Perjalanan kariermu", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                        }
                        Text(if (profileVerified) "Profil lengkap" else "Profil belum lengkap", color = Color.White, fontSize = 12.sp)
                    }
                    Text(
                        "Progres tahap lain belum terverifikasi otomatis.",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    stages.forEachIndexed { index, stage ->
                        val done = index == 0 && profileVerified
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigate(stage.third) }
                                .background(Color.White.copy(alpha = .10f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(31.dp).background(
                                    if (done) mint else Color.White.copy(alpha = .20f),
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (done) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                else Text(stage.first, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(11.dp))
                            Text(stage.second, modifier = Modifier.weight(1f), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Buka tahap", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Career Billboard", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ScreenText)
                if (showTestAd) {
                    // Reserved Career Billboard area: ads are never over bottom navigation.
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        KarirMateTestBanner()
                    }
                } else {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp).clickable { onNavigate(Screen.JobTraining.route) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2CF))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("Upgrade Skill, Buka Peluang", color = Color(0xFF16446B), fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 27.sp)
                                Text("Ikuti pelatihan kerja dari Basic sampai Advanced untuk mengembangkan kemampuanmu.", color = Color(0xFF34546A), fontSize = 13.sp, lineHeight = 19.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.School, contentDescription = null, tint = ocean, modifier = Modifier.size(70.dp))
                        }
                        Button(
                            onClick = { onNavigate(Screen.JobTraining.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = ocean)
                        ) { Text("Lihat Program Pelatihan", color = Color.White) }
                    }
                }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                menu.chunked(4).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { feature ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { onNavigate(feature.route) },
                                shape = RoundedCornerShape(13.dp),
                                colors = CardDefaults.cardColors(containerColor = background),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(37.dp).background(feature.bgTint, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(feature.icon, contentDescription = null, tint = feature.iconTint, modifier = Modifier.size(22.dp))
                                    }
                                    Text(feature.title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = ScreenText, maxLines = 2, minLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onNavigate(Screen.UpgradePro.route) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ocean)) {
                    Text(if (isPro) "Paket PRO" else "Upgrade PRO", fontSize = 12.sp)
                }
            }
        }
        if (isLoading && status == null) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
        }
    }
}

@Composable
private fun DashboardTopHeaderSection(
    isPro: Boolean,
    isOnline: Boolean,
    displayName: String,
    onNavigate: (String) -> Unit
) {
    // Update 5: unified Ocean Blue Dashboard header, matching Update 4.
    // Retains greeting, backend-driven badge, notification and profile navigation.
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.horizontalGradient(listOf(
                Color(0xFF075AA8), Color(0xFF0769B2), Color(0xFF008EAB)
            )))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val upper = Path().apply {
                moveTo(size.width * .64f, 0f)
                cubicTo(size.width * .70f, size.height * .57f,
                    size.width * .91f, size.height * .37f,
                    size.width, size.height * .84f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(upper, Color(0xFF12B2C5).copy(alpha = .28f))
            val lower = Path().apply {
                moveTo(size.width * .23f, size.height)
                cubicTo(size.width * .15f, size.height * .25f,
                    size.width * .44f, size.height * .34f,
                    size.width * .42f, size.height)
                close()
            }
            drawPath(lower, Color(0xFF064787).copy(alpha = .20f))
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        com.example.automatesmartbot.R.drawable.karirmate_brand_mark
                    ),
                    contentDescription = "Logo KarirMate",
                    modifier = Modifier.size(43.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Column(Modifier.weight(1f)) {
                    Text("KarirMate", color = Color.White,
                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
                    Text("AI Career Assistant", color = Color.White.copy(alpha = .85f),
                        fontSize = 10.sp, maxLines = 1)
                }
                com.example.automatesmartbot.ui.components.MembershipBadge(
                    modifier = Modifier.clickable { onNavigate(Screen.Profil.route) },
                    style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.HEADER
                )
                IconButton(
                    onClick = { onNavigate(Screen.Riwayat.route) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Notifications, "Riwayat aktivitas",
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
                IconButton(
                    onClick = { onNavigate(Screen.Profil.route) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Person, "Profil",
                        tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(45.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = .15f))
                        .border(1.dp, Color.White.copy(alpha = .45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White,
                        modifier = Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Halo, $displayName 👋", fontWeight = FontWeight.Bold,
                        fontSize = 17.sp, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Siapkan karier impianmu hari ini", fontSize = 11.sp,
                        color = Color.White.copy(alpha = .88f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(7.dp).background(
                            if (isOnline) Color(0xFF55E6A1) else Color(0xFFFFD56B),
                            CircleShape))
                        Text(if (isOnline) "Online" else "Offline",
                            color = Color.White.copy(alpha = .94f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IndonesianLaborConditionCard(onDetailClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, Color(0xFFF3ECE4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indonesian Flag
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(3.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFE11D48))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(ScreenSurface)
                        )
                    }
                }

                // Title and Subtitle (preserving BPS authentic reference from remote 89adead)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    Text(
                        text = "Kondisi Ketenagakerjaan Indonesia",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = ScreenText
                    )
                    Text(
                        text = "Sumber: Badan Pusat Statistik (BPS) • Update: 3 Nov 2026",
                        fontSize = 10.5.sp,
                        color = ScreenMuted
                    )
                }

                // Detail Data Button
                Surface(
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFF2563A6)),
                    color = ScreenSurface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.clickable(onClick = onDetailClick)
                ) {
                    Text(
                        text = "Detail Data >",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Two Metric Sub-Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sub-Card 1: Pengangguran
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = screenCanvas(Color(0xFFEFF6FF))),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFDBEAFE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    text = "Pengangguran",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ScreenText
                                )
                                Text(
                                    text = "Tingkat Pengangguran Terbuka (TPT)",
                                    fontSize = 8.sp,
                                    color = ScreenMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "7,24 juta orang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = ScreenText
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = screenCanvas(Color(0xFFDCFCE7), Color(0xFF173C2B)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "4,68%",
                                    color = screenCanvas(Color(0xFF166534), Color(0xFF86EFAC)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "▼ -0,08 p.p.",
                                color = screenCanvas(Color(0xFF166534), Color(0xFF86EFAC)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "(vs Feb 2025: 4,76%)",
                            fontSize = 8.sp,
                            color = ScreenMuted
                        )

                        // Sparkline Graph
                        LaborSparkline(color = Color(0xFF3B82F6))
                    }
                }

                // Sub-Card 2: Kemiskinan
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = screenCanvas(Color(0xFFFFF1F2))),
                    border = BorderStroke(1.dp, Color(0xFFFFE4E6)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFFE4E6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    text = "Kemiskinan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ScreenText
                                )
                                Text(
                                    text = "Penduduk Miskin",
                                    fontSize = 8.sp,
                                    color = ScreenMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "22,93 juta orang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            color = ScreenText
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = screenCanvas(Color(0xFFDCFCE7), Color(0xFF173C2B)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "8,07%",
                                    color = screenCanvas(Color(0xFF166534), Color(0xFF86EFAC)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "▼ -0,40 p.p.",
                                color = screenCanvas(Color(0xFF166534), Color(0xFF86EFAC)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "(vs Sep 2025: 8,47%)",
                            fontSize = 8.sp,
                            color = ScreenMuted
                        )

                        // Sparkline Graph
                        LaborSparkline(color = Color(0xFFEC4899))
                    }
                }
            }

            // Bottom Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF2563A6), modifier = Modifier.size(18.dp))
                    Column {
                        Text("Angkatan Kerja", fontSize = 8.5.sp, color = ScreenMuted)
                        Text("154,91 juta", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = ScreenText)
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))

                // Column 2
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                ) {
                    Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF2563A6), modifier = Modifier.size(18.dp))
                    Column {
                        Text("Penduduk Bekerja", fontSize = 8.5.sp, color = ScreenMuted)
                        Text("147,67 juta", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = ScreenText)
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE2E8F0)))

                // Column 3
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1.15f).padding(start = 6.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF2563A6), modifier = Modifier.size(18.dp))
                    Column {
                        Text("Rata-rata Upah", fontSize = 8.5.sp, color = ScreenMuted)
                        Text("Rp3,29 jt/bln", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ScreenText)
                    }
                }
            }
        }
    }

    // Carousel Indicators (Pill + 2 dots)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(5.dp)
                .background(Color(0xFF2563A6), RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(Color(0xFFCBD5E1), CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(Color(0xFFCBD5E1), CircleShape)
        )
    }
}

@Composable
private fun LaborSparkline(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, h * 0.7f)
            cubicTo(w * 0.25f, h * 0.4f, w * 0.45f, h * 0.85f, w * 0.7f, h * 0.35f)
            cubicTo(w * 0.85f, h * 0.15f, w * 0.95f, h * 0.45f, w, h * 0.4f)
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun MenuUtamaSection(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title: Orange 4-square grid icon + "Menu Utama"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = Color(0xFF2563A6),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Menu Utama",
                fontWeight = FontWeight.Bold,
                fontSize = 17.5.sp,
                color = ScreenText
            )
        }

        // 9 Cards in a balanced 3x3 Grid
        primaryMenuItems.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        MenuGridCard(item = item, onClick = { onNavigate(item.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuGridCard(
    item: MainMenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, Color(0xFFF3EAE2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(item.bgTint, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ScreenText,
                textAlign = TextAlign.Center,
                lineHeight = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    fontSize = 8.5.sp,
                    color = ScreenMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CertificateRequirementsCard(
    isPro: Boolean,
    onNavigate: (String) -> Unit
) {
    val req1 = isPro
    val req2 = false
    val req3 = false
    val req4 = false
    val completedCount = listOf(req1, req2, req3, req4).count { it }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(screenCanvas(Color(0xFFFFF7ED)), ScreenSurface)
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column: Framed Certificate Artwork
                    Box(
                        modifier = Modifier
                            .width(108.dp)
                            .height(154.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScreenSurface)
                            .border(1.5.dp, Color(0xFFFFB74D), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .background(Color(0xFF2563A6), RoundedCornerShape(3.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("K", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("KarirMate", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ScreenText)
                            }

                            // Certificate Title
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SERTIFIKAT",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ScreenText,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Penyelesaian Program",
                                    fontSize = 6.5.sp,
                                    color = ScreenMuted
                                )
                            }

                            // Gold Medal Centerpiece
                            Box(
                                modifier = Modifier.size(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val r = size.minDimension / 2
                                    // Golden Ribbon tails
                                    val ribColor = Color(0xFFEF4444)
                                    val p = Path().apply {
                                        moveTo(size.width * 0.38f, size.height * 0.7f)
                                        lineTo(size.width * 0.28f, size.height)
                                        lineTo(size.width * 0.42f, size.height * 0.88f)
                                        lineTo(size.width * 0.56f, size.height)
                                        lineTo(size.width * 0.46f, size.height * 0.7f)
                                        close()
                                    }
                                    drawPath(p, ribColor)

                                    // Outer gold circle
                                    drawCircle(Color(0xFFFBBF24), radius = r * 0.65f)
                                    // Inner circle
                                    drawCircle(Color(0xFFF59E0B), radius = r * 0.52f)
                                    // Center gold star
                                    drawCircle(Color(0xFFFEF3C7), radius = r * 0.32f)
                                }
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Corner flourish placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color(0xFF2563A6).copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    // Right Column: Requirements Checklist
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF2563A6),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Syarat Sertifikat KarirMate",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = ScreenText
                            )
                        }

                        Text(
                            text = "Selesaikan persyaratan berikut untuk mendapatkan sertifikat.",
                            fontSize = 9.5.sp,
                            color = ScreenMuted,
                            lineHeight = 12.sp
                        )

                        // 4 Requirements Checklist
                        RequirementRowItem(
                            icon = Icons.Default.WorkspacePremium,
                            iconTint = Color(0xFFF59E0B),
                            label = "Keanggotaan PRO",
                            isChecked = req1,
                            onClick = { onNavigate(Screen.Profil.route) }
                        )

                        RequirementRowItem(
                            icon = Icons.Default.School,
                            iconTint = Color(0xFF3B82F6),
                            label = "Pelatihan Kerja (sesuai posisi)",
                            isChecked = req2,
                            onClick = { onNavigate(Screen.JobTraining.route) }
                        )

                        RequirementRowItem(
                            icon = Icons.Default.Person,
                            iconTint = Color(0xFF9333EA),
                            label = "AI Interview",
                            isChecked = req3,
                            onClick = { onNavigate(Screen.AiInterview.route) }
                        )

                        RequirementRowItem(
                            icon = Icons.Default.Psychology,
                            iconTint = Color(0xFFEC4899),
                            label = "Psikotes Campuran",
                            isChecked = req4,
                            onClick = { onNavigate(Screen.LatihanPsikotes.route) }
                        )

                        // Progress counter & authentic server verification note from remote 89adead
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Progress: $completedCount/4",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = ScreenText
                            )
                            if (completedCount < 4) {
                                Text(
                                    text = "Menunggu verifikasi server",
                                    fontSize = 9.sp,
                                    color = ScreenMuted
                                )
                            }
                        }

                        // 4-Segment Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 1..4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (completedCount >= i) Color(0xFF2563A6) else Color(0xFFCBD5E1))
                                )
                            }
                        }
                    }
                }

                // Action Button: "Buat Sertifikat"
                Button(
                    onClick = {
                        if (completedCount >= 4) {
                            onNavigate(Screen.JobTraining.route)
                        } else {
                            // If missing requirements, guide user to first incomplete requirement
                            when {
                                !req1 -> onNavigate(Screen.Profil.route)
                                !req2 -> onNavigate(Screen.JobTraining.route)
                                !req3 -> onNavigate(Screen.AiInterview.route)
                                else -> onNavigate(Screen.LatihanPsikotes.route)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (completedCount >= 4) Color(0xFF2563A6) else Color(0xFF94A3B8),
                        contentColor = com.example.automatesmartbot.ui.theme.OnKarirOrange
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (completedCount >= 4) Icons.Default.WorkspacePremium else Icons.Default.Lock,
                            contentDescription = null,
                            tint = com.example.automatesmartbot.ui.theme.OnKarirOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Buat Sertifikat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = com.example.automatesmartbot.ui.theme.OnKarirOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequirementRowItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = ScreenText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = 1.2.dp,
                    color = if (isChecked) Color(0xFF2563A6) else Color(0xFF55C6A9),
                    shape = RoundedCornerShape(4.dp)
                )
                .background(if (isChecked) Color(0xFF2563A6) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileIncompleteBanner(onCompleteProfile: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, Color(0xFFFFD8B2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text("Profil belum lengkap", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ScreenText)
                    Text("Lengkapi data diri untuk optimasi rekomendasi karier.", fontSize = 10.sp, color = ScreenMuted)
                }
            }
            TextButton(
                onClick = onCompleteProfile,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Lengkapi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
