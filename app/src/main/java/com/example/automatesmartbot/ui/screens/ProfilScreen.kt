package com.example.automatesmartbot.ui.screens

import androidx.compose.material3.IconButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.model.ProfileResponse
import com.example.automatesmartbot.data.model.UserMeResponse
import com.example.automatesmartbot.data.repository.AuthRepository
import com.example.automatesmartbot.data.repository.ProfileSetupRepository
import com.example.automatesmartbot.ui.components.CvSection
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.ui.theme.karirButtonColors
import com.example.automatesmartbot.ui.theme.ScreenAccent as KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.ScreenPageGradient as KarirPageGradient
import com.example.automatesmartbot.ui.theme.ScreenText as KarirTextDark
import com.example.automatesmartbot.ui.theme.ScreenMuted as KarirTextMuted
import com.example.automatesmartbot.ui.theme.ThemeMode
import com.example.automatesmartbot.ui.theme.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun ProfilScreen(
    isOnline: Boolean = true,
    onNavigateLogin: () -> Unit,
    onNavigateEditProfile: () -> Unit,
    themePreference: ThemePreference,
    onNavigateCv: () -> Unit = {},
    onNavigateApplications: () -> Unit = {},
    onNavigateDocuments: () -> Unit = {},
    onNavigateCertificates: () -> Unit = {},
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileSetupRepository = remember { ProfileSetupRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val photoPrefs = remember(context) { context.getSharedPreferences("karirmate_profile_photo", android.content.Context.MODE_PRIVATE) }
    var photoUri by remember { mutableStateOf(photoPrefs.getString("uri", null)) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoPrefs.edit().putString("uri", uri.toString()).apply()
            photoUri = uri.toString()
        }
    }
    var profile by remember { mutableStateOf<ProfileResponse?>(null) }
    var account by remember { mutableStateOf<UserMeResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun fetchProfileData() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val accountResult = authRepository.getMe()
            val result = profileRepository.fetchProfile()
            isLoading = false
            accountResult.onSuccess { account = it }
            result.fold(
                onSuccess = { prof ->
                    profile = prof
                },
                onFailure = { err ->
                    errorMessage = err.localizedMessage ?: "Gagal memuat profil."
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        fetchProfileData()
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
    ) {
        if (isLoading && profile == null && errorMessage == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KarirOrange)
            }
        } else if (errorMessage != null && profile == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isOnline) "Gagal memuat profil" else "Anda sedang offline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isOnline) {
                        errorMessage ?: "Terjadi kesalahan. Coba lagi."
                    } else {
                        "Sesi Anda tetap tersimpan. Sambungkan internet lalu coba lagi."
                    },
                    fontSize = 13.sp,
                    color = KarirTextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { fetchProfileData() },
                    colors = karirButtonColors(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Coba Lagi", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onNavigateLogin) {
                    Text("Masuk / Login", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "KARIRMATE • PROFIL",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = screenCanvas(Color(0xFFE8F5F7)),
                                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8)))
                            ) {
                                Text(
                                    "DATA UTAMA",
                                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                        Text(
                            "Profil Saya",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = KarirTextDark
                        )
                        Text(
                            "Kelola data diri, kontak, preferensi, dan kelengkapan CV Anda.",
                            color = KarirTextMuted,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Box(Modifier.padding(horizontal = 16.dp)) {
                    ProfileIdentityCard(
                        profile = profile,
                        account = account,
                        errorMessage = errorMessage,
                        photoUri = photoUri,
                        onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                        onNavigateEditProfile = onNavigateEditProfile,
                        onNavigateCv = onNavigateCv,
                        onNavigateApplications = onNavigateApplications,
                        onNavigateDocuments = onNavigateDocuments,
                        onNavigateCertificates = onNavigateCertificates,
                        onLogout = { authRepository.logout(); onNavigateLogin() }
                    )
                }

                Box(Modifier.padding(horizontal = 16.dp)) {
                    CvSection()
                }

                Box(Modifier.padding(horizontal = 16.dp)) {
                    AppearanceCard(
                        selectedMode = themePreference.mode,
                        onModeSelected = themePreference::selectMode
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppearanceCard(selectedMode: ThemeMode, onModeSelected: (ThemeMode) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Tema Tampilan", fontWeight = FontWeight.Bold, color = KarirTextDark, fontSize = 16.sp)
            Text("Pilih mode tampilan Karir Mate yang nyaman bagi Anda.", color = KarirTextMuted, fontSize = 12.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = selectedMode == mode
                    androidx.compose.material3.FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selected,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.label, maxLines = 1, fontSize = 11.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = screenCanvas(Color(0xFFE8F5F7)),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = ScreenSurface,
                            labelColor = KarirTextDark
                        ),
                        border = BorderStroke(1.dp, if (selected) KarirOrange else screenCanvas(Color(0xFFC6DFE8)))
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 13.sp, color = KarirTextMuted)
        }
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarirTextDark,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ProfileIdentityCard(
    profile: ProfileResponse?,
    account: UserMeResponse?,
    errorMessage: String?,
    photoUri: String?,
    onPickPhoto: () -> Unit,
    onNavigateEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onNavigateCv: () -> Unit = {},
    onNavigateApplications: () -> Unit = {},
    onNavigateDocuments: () -> Unit = {},
    onNavigateCertificates: () -> Unit = {}
) {
    val context = LocalContext.current
    val profileFields = listOf(profile?.fullName, profile?.whatsapp, profile?.domicile, profile?.education, profile?.applicationEmail)
    val completeCount = profileFields.count { !it.isNullOrBlank() } + listOf(profile?.desiredPositions, profile?.preferredLocations).count { !it.isNullOrEmpty() }
    val completion = completeCount / 7f
    val avatar = remember(photoUri) { runCatching { photoUri?.let { context.contentResolver.openInputStream(Uri.parse(it))?.use(BitmapFactory::decodeStream)?.asImageBitmap() } }.getOrNull() }

    Card(
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = screenCanvas(Color(0xFFE8F5F7)),
                    border = BorderStroke(1.5.dp, screenCanvas(Color(0xFFC6DFE8))),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (avatar != null) {
                            Image(
                                bitmap = avatar,
                                contentDescription = "Foto profil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Foto profil belum tersedia",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        profile?.fullName?.takeIf { it.isNotBlank() } ?: "Nama belum diisi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = KarirTextDark
                    )
                    Surface(
                        color = screenCanvas(Color(0xFFE8F5F7)),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Pencari Kerja",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(onClick = onNavigateEditProfile) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit profil",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            TextButton(onClick = onPickPhoto) {
                Text(
                    "📷  ${if (photoUri == null) "Tambah Foto Profil" else "Ganti Foto Profil"}",
                    color = KarirOrangeDeep,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = screenCanvas(Color(0xFFF5FAFC))),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kelengkapan Profil", color = KarirTextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${(completion * 100).toInt()}%", color = screenCanvas(Color(0xFF166534), Color(0xFF86EFAC)), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    LinearProgressIndicator(
                        progress = { completion },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFFFFE4D1)
                    )
                    Text(
                        if (completeCount == 7) "✓ Profil Anda sudah lengkap dan optimal!"
                        else "Lengkapi ${7 - completeCount} bagian profil untuk memaksimalkan peluang kerja.",
                        color = if (completeCount == 7) Color(0xFF16A34A) else KarirTextMuted,
                        fontSize = 11.5.sp
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (profile?.completed == true) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (profile?.completed == true) "PROFIL LENGKAP" else "PROFIL BELUM LENGKAP",
                        color = if (profile?.completed == true) Color(0xFF166534) else Color(0xFF92400E),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    color = when (account?.isEmailVerified) {
                        true -> Color(0xFFDCFCE7)
                        false -> Color(0xFFFEF3C7)
                        null -> screenCanvas(Color(0xFFF1F5F9))
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (account?.isEmailVerified) {
                            true -> "EMAIL TERVERIFIKASI"
                            false -> "EMAIL BELUM VERIFIKASI"
                            null -> "EMAIL TIDAK DIKETAHUI"
                        },
                        color = when (account?.isEmailVerified) {
                            true -> Color(0xFF166534)
                            false -> Color(0xFF92400E)
                            null -> KarirTextMuted
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = screenCanvas(Color(0xFFC6DFE8)))

            Text(
                "INFORMASI PRIBADI",
                color = KarirOrangeDeep,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            ProfileDetailRow(icon = Icons.Default.Email, label = "Email", value = account?.email ?: "-")
            ProfileDetailRow(icon = Icons.Default.Phone, label = "No. HP / WhatsApp", value = profile?.whatsapp ?: "-")
            ProfileDetailRow(icon = Icons.Default.LocationOn, label = "Domisili", value = profile?.domicile ?: "-")
            ProfileDetailRow(icon = Icons.Default.School, label = "Pendidikan", value = profile?.education ?: "-")

            HorizontalDivider(color = screenCanvas(Color(0xFFC6DFE8)))

            Text(
                "PREFERENSI KARIER",
                color = KarirOrangeDeep,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )

            ProfileDetailRow(
                icon = Icons.Default.Work,
                label = "Posisi Diminati",
                value = profile?.desiredPositions?.joinToString(", ") ?: "-"
            )
            ProfileDetailRow(
                icon = Icons.Default.LocationOn,
                label = "Lokasi Kerja",
                value = profile?.preferredLocations?.joinToString(", ") ?: "-"
            )
            ProfileDetailRow(
                icon = Icons.Default.Email,
                label = "Email Lamaran",
                value = profile?.applicationEmail ?: "-"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = screenCanvas(Color(0xFFF5FAFC))),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ringkasan Karier", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KarirTextDark)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CareerSummaryTile("CV", "Buka CV", screenCanvas(Color(0xFFE8F5F7)), Modifier.weight(1f))
                        CareerSummaryTile("Lamaran", "Riwayat", screenCanvas(Color(0xFFE8F5F7)), Modifier.weight(1f))
                        CareerSummaryTile("Dokumen", "Kelola", screenCanvas(Color(0xFFE8F5F7)), Modifier.weight(1f))
                    }
                    Text("Ringkasan integrasi fitur resmi KarirMate.", fontSize = 11.sp, color = KarirTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("AKSES CEPAT", fontWeight = FontWeight.Bold, color = KarirTextDark, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNavigateCv, modifier = Modifier.weight(1f)) { Text("CV Saya", fontSize = 12.sp) }
                OutlinedButton(onClick = onNavigateApplications, modifier = Modifier.weight(1f)) { Text("Lamaran", fontSize = 12.sp) }
                OutlinedButton(onClick = onNavigateDocuments, modifier = Modifier.weight(1f)) { Text("Dokumen", fontSize = 12.sp) }
            }
            OutlinedButton(
                onClick = onNavigateCertificates,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sertifikat Saya — Riwayat & Unduh", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateEditProfile,
                    colors = karirButtonColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Profil", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ScreenSurface,
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Keluar", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CareerSummaryTile(title: String, caption: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color,
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("—", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarirOrange)
            Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = KarirTextDark)
            Text(caption, fontSize = 10.sp, color = KarirTextMuted)
        }
    }
}
