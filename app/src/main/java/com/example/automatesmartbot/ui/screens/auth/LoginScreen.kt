package com.example.automatesmartbot.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
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
import com.example.automatesmartbot.ui.components.BrandLogo
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.ui.theme.karirButtonColors
import com.example.automatesmartbot.ui.theme.ScreenAccent as KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.ScreenPageGradient as KarirPageGradient
import com.example.automatesmartbot.ui.theme.ScreenText as KarirTextDark
import com.example.automatesmartbot.ui.theme.ScreenMuted as KarirTextMuted
import com.example.automatesmartbot.ui.viewmodel.AuthViewModel

@Composable
private fun LoginScreenContent(
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    onLoginSuccess: (profileCompleted: Boolean?) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("karirmate_login_preferences", 0) }
    var rememberMe by remember { mutableStateOf(prefs.getString("remembered_email", "").orEmpty().isNotBlank()) }
    var googleNotice by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { email = prefs.getString("remembered_email", "").orEmpty() }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val textFieldColors = com.example.automatesmartbot.ui.theme.karirInputColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ScreenSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BrandLogo(
                    modifier = Modifier.size(92.dp)
                )
                Text("KarirMate", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = KarirTextDark)
                Text("Your Career Journey", color = KarirTextMuted, fontSize = 13.sp)

                Text(
                    text = "Masuk ke Akunmu",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = KarirTextDark
                )

                Text(
                    text = "Lanjutkan persiapan kariermu",
                    fontSize = 12.5.sp,
                    color = KarirTextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email atau No. HP") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Sembunyikan sandi" else "Tampilkan sandi",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                        Text("Ingat Saya", color = KarirTextDark, fontSize = 13.sp)
                    }
                    TextButton(onClick = onNavigateForgot) {
                        Text("Lupa kata sandi?", fontSize = 12.5.sp, color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        prefs.edit().putString("remembered_email", if (rememberMe) email else "").apply()
                        viewModel.login(email, password, onLoginSuccess)
                    },
                    enabled = !isLoading,
                    colors = karirButtonColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Text("atau masuk dengan", color = KarirTextMuted, fontSize = 13.sp)
                OutlinedButton(onClick = { googleNotice = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Masuk dengan Google", color = KarirTextDark, fontWeight = FontWeight.SemiBold)
                }
                if (googleNotice) Text("Login Google belum tersedia: perlu integrasi OAuth.", color = KarirTextMuted, fontSize = 12.sp)

                errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                successMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = screenCanvas(Color(0xFFDCFCE7), Color(0xFF173C2B)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = screenCanvas(Color(0xFF166534), Color(0xFFA7F3D0)),
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
                    Text("Belum punya akun?", fontSize = 13.sp, color = KarirTextMuted)
                    TextButton(onClick = onNavigateRegister) {
                        Text("Daftar Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KarirOrangeDeep)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onNavigateRegister: () -> Unit,
    onNavigateForgot: () -> Unit,
    onLoginSuccess: (profileCompleted: Boolean?) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    com.example.automatesmartbot.ui.components.AuthLightTheme {
        LoginScreenContent(onNavigateRegister, onNavigateForgot, onLoginSuccess, viewModel)
    }
}
