package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.ui.components.CvSection
import com.example.automatesmartbot.ui.viewmodel.ProfileSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    onBack: () -> Unit,
    onProfileSaved: () -> Unit,
    isOnline: Boolean = true,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var domicile by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var desiredPositions by remember { mutableStateOf("") }
    var preferredLocations by remember { mutableStateOf("") }
    var applicationEmail by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        profile?.let { p ->
            fullName = p.fullName ?: ""
            whatsapp = p.whatsapp ?: ""
            domicile = p.domicile ?: ""
            education = p.education ?: ""
            desiredPositions = p.desiredPositions?.joinToString(", ") ?: ""
            preferredLocations = p.preferredLocations?.joinToString(", ") ?: ""
            applicationEmail = p.applicationEmail ?: ""
        }
    }

    val scrollState = rememberScrollState()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF2563A6),
        unfocusedBorderColor = Color(0xFF55C6A9),
        focusedContainerColor = Color(0xFFF5FAFC),
        unfocusedContainerColor = Color(0xFFF5FAFC),
        focusedTextColor = Color(0xFF243547),
        unfocusedTextColor = Color(0xFF243547)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                title = { Text("Setup / Edit Profil", color = Color(0xFF243547), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFFFF))
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFFFFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveProfile(
                                fullName,
                                whatsapp,
                                domicile,
                                education,
                                desiredPositions,
                                preferredLocations,
                                applicationEmail,
                                onProfileSaved
                            )
                        },
                        enabled = isOnline && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563A6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    if (!isOnline) {
                        Text(
                            text = "Anda sedang offline. Sambungkan internet untuk menyimpan profil.",
                            fontSize = 11.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF5FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Field Bertanda (*) Wajib Diisi",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563A6)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("Nomor WhatsApp *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2563A6)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = domicile,
                        onValueChange = { domicile = it },
                        label = { Text("Lokasi / Domisili *") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2563A6)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = education,
                        onValueChange = { education = it },
                        label = { Text("Pendidikan Terakhir * (e.g. S1, D3)") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF2563A6)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = desiredPositions,
                        onValueChange = { desiredPositions = it },
                        label = { Text("Posisi Pekerjaan Diminati (Pisah koma)") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = Color(0xFF2563A6)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = preferredLocations,
                        onValueChange = { preferredLocations = it },
                        label = { Text("Lokasi Penempatan Kerja (Pisah koma)") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2563A6)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = applicationEmail,
                        onValueChange = { applicationEmail = it },
                        label = { Text("Email Pengirim Lamaran *") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2563A6)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            CvSection()

            errorMessage?.let { err ->
                Text(
                    text = err,
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }

            successMessage?.let { msg ->
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
