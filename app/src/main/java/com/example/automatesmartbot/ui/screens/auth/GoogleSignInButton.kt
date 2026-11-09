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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private const val WEB_CLIENT_ID =
    "39856526801-srp1fvapkep1k67785qo3slk5ilqfpka.apps.googleusercontent.com"

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
                        val option = GetSignInWithGoogleOption
                            .Builder(WEB_CLIENT_ID)
                            .build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(option)
                            .build()
                        val credential = CredentialManager.create(context)
                            .getCredential(context, request)
                            .credential

                        if (credential !is CustomCredential ||
                            credential.type !=
                            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            error("Google tidak mengirim identitas yang valid.")
                        }

                        val googleToken =
                            GoogleIdTokenCredential.createFrom(
                                credential.data
                            ).idToken

                        message = "Akun dipilih. Memverifikasi login KarirMate..."
                        onIdToken(googleToken)
                    } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                        message = "Pemilihan akun Google dibatalkan atau tidak selesai."
                    } catch (e: Exception) {
                        message = e.localizedMessage
                            ?: "Gagal membuka Google Sign-In."
                    } finally {
                        busy = false
                    }
                }
            }
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp))
            } else {
                Text(label)
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else if (message.isNotBlank()) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
