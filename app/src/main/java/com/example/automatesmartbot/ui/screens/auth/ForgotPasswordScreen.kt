package com.example.automatesmartbot.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                title = { Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B263B))
            )
        },
        containerColor = Color(0xFF0D1B2A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Reset Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )

                    Text(
                        text = "Masukkan email akun Anda untuk pemulihan password.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF60A5FA)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0D1B2A),
                            unfocusedContainerColor = Color(0xFF0D1B2A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            infoMessage = "Belum tersedia"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Kirim Instruksi Reset", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    infoMessage?.let { msg ->
                        Text(text = msg, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }

                    TextButton(onClick = onNavigateLogin) {
                        Text("Kembali ke Masuk", fontSize = 13.sp, color = Color(0xFF60A5FA))
                    }
                }
            }
        }
    }
}
