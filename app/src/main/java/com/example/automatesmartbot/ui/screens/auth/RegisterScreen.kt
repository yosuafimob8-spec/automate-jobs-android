package com.example.automatesmartbot.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontStyle
import com.example.automatesmartbot.ui.components.BrandLogo
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import com.example.automatesmartbot.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreenContent(
    onNavigateLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onGoogleSuccess: (Boolean?) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KarirOrange,
        unfocusedBorderColor = Color(0xFFC6DFE8),
        focusedLabelColor = KarirOrange,
        unfocusedLabelColor = KarirTextMuted,
        cursorColor = KarirOrange,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedTextColor = KarirTextDark,
        unfocusedTextColor = KarirTextDark
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.border(width = 0.75.dp, color = Color(0xFFC6DFE8)),
                navigationIcon = {
                    IconButton(onClick = onNavigateLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = KarirTextDark
                        )
                    }
                },
                title = { Text("Daftar Akun", color = KarirTextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5FAFC))
            )
        },
        containerColor = Color(0xFFF5FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KarirPageGradient)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandLogo(modifier = Modifier.size(72.dp))
                    Text("KarirMate", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = KarirTextDark)
                    Text("Your Career Journey", fontSize = 13.sp, color = KarirTextMuted)
                    Text(
                        text = "Buat Akun Baru",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = KarirTextDark
                    )

                    Text(
                        text = "Gabung dan mulai perjalanan kariermu",
                        fontSize = 12.5.sp,
                        color = KarirTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = KarirOrange) },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = KarirOrange) },
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Kata Sandi (min. 8 karakter)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KarirOrange) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = KarirTextMuted
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Kata Sandi") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KarirOrange) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                        Text("Saya setuju dengan Syarat & Ketentuan dan Kebijakan Privasi", color = KarirTextDark, fontSize = 13.sp)
                    }
                    if (notice.isNotBlank()) Text(notice, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Button(
                        onClick = {
                            if (name.isBlank()) notice = "Nama lengkap wajib diisi."
                            else if (!agreed) notice = "Setujui Syarat & Ketentuan dan Kebijakan Privasi."
                            else { notice = ""; viewModel.register(email, password, confirmPassword, onRegisterSuccess) }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Daftar Sekarang", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Text(
                        "atau daftar dengan Google",
                        color = KarirTextMuted,
                        fontSize = 13.sp
                    )
                    GoogleSignInButton(
                        label = "Daftar dengan Google",
                        enabled = !isLoading && agreed,
                        onIdToken = {
                            viewModel.loginWithGoogle(it, onGoogleSuccess)
                        }
                    )

                    errorMessage?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    successMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sudah punya akun?", fontSize = 13.sp, color = KarirTextMuted)
                        TextButton(onClick = onNavigateLogin) {
                            Text("Masuk", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KarirOrangeDeep)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onNavigateLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onGoogleSuccess: (Boolean?) -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    com.example.automatesmartbot.ui.components.AuthLightTheme {
        RegisterScreenContent(
            onNavigateLogin,
            onRegisterSuccess,
            onGoogleSuccess,
            viewModel
        )
    }
}
