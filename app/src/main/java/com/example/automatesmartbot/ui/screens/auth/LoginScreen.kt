package com.example.automatesmartbot.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    onLoginSuccess: (profileCompleted: Boolean?) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
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
                    text = "Masuk AutoMate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White
                )

                Text(
                    text = "Masukkan email dan password akun terverifikasi Anda.",
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

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateForgot) {
                        Text("Lupa Password?", fontSize = 13.sp, color = Color(0xFF60A5FA))
                    }
                }

                Button(
                    onClick = {
                        viewModel.login(email, password, onLoginSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                errorMessage?.let { err ->
                    Text(text = err, fontSize = 12.sp, color = Color(0xFFEF4444))
                }

                successMessage?.let { msg ->
                    Text(text = msg, fontSize = 12.sp, color = Color(0xFF34D399))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Belum punya akun?", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    TextButton(onClick = onNavigateRegister) {
                        Text("Daftar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                    }
                }
            }
        }
    }
}
