package com.example.automatesmartbot.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.repository.AuthRepository
import kotlinx.coroutines.launch

/** OTP_AUTO_GATE_V1. Fullscreen, modal; can't skip to dashboard. */
@Composable
fun EmailOtpGate(
    state: String,
    email: String,
    onVerified: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    var code by rememberSaveable(email) { mutableStateOf("") }
    var sending by remember(email) { mutableStateOf(false) }
    var confirming by remember(email) { mutableStateOf(false) }
    var sent by remember(email) { mutableStateOf(false) }
    var message by remember(email) { mutableStateOf("") }
    var error by remember(email) { mutableStateOf("") }

    // Auto-request exactly once when entering this gate (resend is manual and rate-limited).
    LaunchedEffect(state, email) {
        if (state == "unverified") {
            sending = true
            error = ""
            val result = repository.requestEmailVerification()
            result.fold(
                onSuccess = {
                    sent = true
                    message = "Kode OTP enam digit dikirim ke email akun. Berlaku 10 menit."
                },
                onFailure = {
                    sent = false
                    error = it.localizedMessage ?: "Pengiriman OTP gagal."
                }
            )
            sending = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("KarirMate", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text("Verifikasi Email", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (state == "checking") {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Memeriksa status verifikasi email…")
        } else if (state == "error") {
            Text("Tidak dapat memeriksa status email dari server. Akses dashboard ditahan sampai status terverifikasi.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Coba Lagi") }
        } else if (state == "unverified") {
            Text("Masukkan OTP yang dikirim ke:")
            Text(email.ifBlank { "email akun Anda" }, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            if (sending) CircularProgressIndicator()
            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
            }
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = code,
                onValueChange = { value -> code = value.filter { it.isDigit() }.take(6) },
                label = { Text("Kode OTP (6 digit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !confirming && !sending && code.length == 6,
                onClick = {
                    scope.launch {
                        confirming = true
                        error = ""
                        val result = repository.confirmEmailVerification(code)
                        result.fold(
                            onSuccess = {
                                // Verify via /auth/me, not only an HTTP 200 response.
                                val account = repository.getMe().getOrNull()
                                if (account?.isEmailVerified == true) onVerified()
                                else error = "Server belum mengonfirmasi status email. Coba periksa lagi."
                            },
                            onFailure = {
                                error = it.localizedMessage ?: "Kode salah atau kedaluwarsa."
                            }
                        )
                        confirming = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (confirming) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else Text("Verifikasi & Lanjutkan")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        sending = true
                        error = ""
                        repository.requestEmailVerification().fold(
                            onSuccess = {
                                sent = true
                                message = "OTP baru dikirim. Berlaku 10 menit."
                                code = ""
                            },
                            onFailure = { error = it.localizedMessage ?: "Gagal mengirim ulang OTP." }
                        )
                        sending = false
                    }
                },
                enabled = !sending && !confirming,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Kirim Ulang OTP") }
            if (!sent) Text("Jika OTP belum masuk, periksa Spam atau konfigurasi SMTP server.")
        }
        Spacer(Modifier.height(22.dp))
        TextButton(onClick = onLogout) { Text("Keluar / Gunakan akun lain") }
    }
}
