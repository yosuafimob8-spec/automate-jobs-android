package com.example.automatesmartbot.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Public OAuth Web client ID; must match the verified audience on /auth/google backend.
private const val WEB_CLIENT_ID = "39856526801-srp1fvapkep1k67785qo3slk5ilqfpka.apps.googleusercontent.com"

/** Real Google identity flow. No simulated login or local-only success path. */
@Composable
internal fun GoogleSignInButton(
    label: String,
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    errorMessage: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            enabled = enabled && !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                busy = true
                message = ""
                scope.launch {
                    try {
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build())
                            .build()
                        val credential = CredentialManager.create(context)
                            .getCredential(context = context, request = request).credential
                        require(credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) { "Google tidak memberikan token identitas yang valid." }
                        val googleToken = GoogleIdTokenCredential
                            .createFrom(credential.data).idToken
                        require(googleToken.isNotBlank()) { "Token Google kosong." }
                        // Backend verifies signature, issuer, audience and expiry, then creates session.
                        // Selecting an account must NOT be treated as a successful login.
                        message = "Akun dipilih. Memverifikasi dengan server KarirMate..."
                        onIdToken(googleToken)
                    } catch (e: GetCredentialCancellationException) {
                        message = "Pemilihan akun Google dibatalkan."
                    } catch (e: GetCredentialException) {
                        message = "Google Sign-In tidak berhasil. Periksa layanan Google Play dan konfigurasi aplikasi."
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        message = e.localizedMessage ?: "Google Login gagal. Coba lagi."
                    } finally {
                        busy = false
                    }
                }
            }
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp))
            else Text(label)
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        } else if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
