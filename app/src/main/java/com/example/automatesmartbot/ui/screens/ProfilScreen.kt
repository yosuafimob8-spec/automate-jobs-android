package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.automatesmartbot.ui.theme.ThemeMode
import com.example.automatesmartbot.ui.theme.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun ProfilScreen(
    isOnline: Boolean = true,
    onNavigateLogin: () -> Unit,
    onNavigateEditProfile: () -> Unit,
    themePreference: ThemePreference,
    authRepository: AuthRepository = remember { AuthRepository() },
    profileRepository: ProfileSetupRepository = remember { ProfileSetupRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
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
            .background(Color(0xFF0D1B2A))
            .padding(16.dp)
    ) {
        if (isLoading && profile == null && errorMessage == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else if (errorMessage != null && profile == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isOnline) "Gagal memuat profil" else "Anda sedang offline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isOnline) {
                        errorMessage ?: "Terjadi kesalahan. Coba lagi."
                    } else {
                        "Sesi Anda tetap tersimpan. Sambungkan internet lalu coba lagi."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { fetchProfileData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Coba Lagi")
                }
                TextButton(onClick = onNavigateLogin) {
                    Text("Masuk / Login", color = Color(0xFF60A5FA))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF22304A),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = profile?.fullName ?: "Pengguna",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )

                        Surface(
                            color = if (profile?.completed == true) Color(0xFF065F46) else Color(0xFF5F0606),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (profile?.completed == true) "PROFIL LENGKAP" else "PROFIL BELUM LENGKAP",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = when (account?.isEmailVerified) {
                                true -> Color(0xFF065F46)
                                false -> Color(0xFF5F0606)
                                null -> Color(0xFF334155)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when (account?.isEmailVerified) {
                                    true -> "EMAIL TERVERIFIKASI"
                                    false -> "EMAIL BELUM TERVERIFIKASI"
                                    null -> "STATUS EMAIL TIDAK DIKETAHUI"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        ProfileDetailRow(icon = Icons.Default.Email, label = "Email Akun", value = account?.email ?: "-")
                        ProfileDetailRow(icon = Icons.Default.Phone, label = "WhatsApp", value = profile?.whatsapp ?: "-")
                        ProfileDetailRow(icon = Icons.Default.LocationOn, label = "Domisili", value = profile?.domicile ?: "-")
                        ProfileDetailRow(icon = Icons.Default.School, label = "Pendidikan", value = profile?.education ?: "-")
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateEditProfile,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Profil", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    authRepository.logout()
                                    onNavigateLogin()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Keluar", fontSize = 13.sp)
                            }
                        }

                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                fontSize = 12.sp,
                                color = Color(0xFFF87171),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                CvSection()

                AppearanceCard(
                    selectedMode = themePreference.mode,
                    onModeSelected = themePreference::selectMode
                )
            }
        }
    }
}

@Composable
private fun AppearanceCard(selectedMode: ThemeMode, onModeSelected: (ThemeMode) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Tampilan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Text("Pilih mode warna Karir Mate.", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    androidx.compose.material3.FilterChip(
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.label, maxLines = 1, fontSize = 11.sp) },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1267E8),
                            selectedLabelColor = Color.White
                        )
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 13.sp, color = Color(0xFF94A3B8))
        }
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
