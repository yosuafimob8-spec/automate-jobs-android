package com.example.automatesmartbot.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KarirOrange,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = KarirOrange,
        unfocusedLabelColor = KarirTextMuted,
        cursorColor = KarirOrange,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedTextColor = KarirTextDark,
        unfocusedTextColor = KarirTextDark
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.border(width = 0.75.dp, color = Color(0xFFF1E5DC)),
                navigationIcon = {
                    IconButton(onClick = onNavigateLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = KarirTextDark
                        )
                    }
                },
                title = { Text("Reset Password", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF9))
            )
        },
        containerColor = Color(0xFFFFFBF7)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Reset Password Akun",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Masukkan alamat email terdaftar Anda untuk menerima tautan pemulihan kata sandi.",
                        fontSize = 12.5.sp,
                        color = KarirTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Terdaftar") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = KarirOrange) },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            infoMessage = "Fitur reset password melalui email sedang disiapkan di server."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Kirim Tautan Reset", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    infoMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                color = Color(0xFF9A3412),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    TextButton(onClick = onNavigateLogin) {
                        Text("Kembali ke Halaman Masuk", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
