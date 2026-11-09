package com.example.automatesmartbot.ui.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.psychotest.PsychotestPolicy
import com.example.automatesmartbot.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

internal data class CertificateScoreReadiness(
    val psychScore: Int? = null,
    val voiceScore: Int? = null,
    val psychStatus: String = "Memuat nilai Tes Gabungan...",
    val voiceStatus: String = "Memuat nilai Interview Suara..."
) {
    val psychPassed get() = (psychScore ?: -1) >= 80
    val voicePassed get() = (voiceScore ?: -1) >= 80
}

internal object CertificateScoreBridge {
    suspend fun accountKey(): String? {
        val email = AuthRepository().getMe()
            .getOrNull()?.email?.trim()?.lowercase()
            ?.takeIf { it.isNotBlank() } ?: return null
        return MessageDigest.getInstance("SHA-256")
            .digest(email.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            "career_certificate_voice_scores",
            Context.MODE_PRIVATE
        )

    fun saveVoice(context: Context, account: String?, score: Int) {
        if (account == null || score !in 0..100) return
        val prefs = prefs(context)
        val old = prefs.getInt(account, -1)
        if (score > old) {
            prefs.edit().putInt(account, score).apply()
        }
    }

    fun readVoice(context: Context, account: String?): Int? {
        if (account == null) return null
        return prefs(context).getInt(account, -1)
            .takeIf { it in 0..100 }
    }
}

@Composable
internal fun rememberCertificateScores(): CertificateScoreReadiness {
    val context = LocalContext.current
    val token = ApiClient.getSessionManager()?.getValidToken()
    val api = ApiClient.psychotestApi

    var result by remember(token) {
        mutableStateOf(CertificateScoreReadiness())
    }

    LaunchedEffect(token, api) {
        if (token.isNullOrBlank()) {
            result = CertificateScoreReadiness(
                psychStatus = "Login untuk melihat hasil",
                voiceStatus = "Login untuk melihat hasil"
            )
            return@LaunchedEffect
        }

        val account = runCatching {
            CertificateScoreBridge.accountKey()
        }.getOrNull()

        val voice = CertificateScoreBridge.readVoice(
            context, account
        )

        result = result.copy(
            voiceScore = voice,
            voiceStatus = when {
                account == null -> "Akun belum terverifikasi"
                voice == null -> "Belum ada hasil Interview Suara"
                else -> "Skor terbaik: $voice/100 (Interview Suara)"
            }
        )

        if (api == null) {
            result = result.copy(
                psychStatus = "Layanan Psikotes belum tersedia"
            )
            return@LaunchedEffect
        }

        try {
            val scores = mutableListOf<Int>()
            // Baca beberapa halaman agar Tes Gabungan lama tetap ditemukan.
            for (offset in listOf(0, 100, 200)) {
                val response = withContext(Dispatchers.IO) {
                    api.history(limit = 100, offset = offset)
                }
                if (!response.isSuccessful) {
                    result = result.copy(
                        psychStatus = "Riwayat Psikotes HTTP ${response.code()}"
                    )
                    return@LaunchedEffect
                }
                val items = response.body()
                    ?.getAsJsonArray("items")
                    ?: break

                for (element in items) {
                    val item = element.asJsonObject
                    val id = item.get("test_id")
                        ?.takeUnless { it.isJsonNull }
                        ?.asString.orEmpty().lowercase()
                    val title = item.get("title")
                        ?.takeUnless { it.isJsonNull }
                        ?.asString.orEmpty().lowercase()

                    // Hanya Tes Gabungan, bukan tes kategori lainnya.
                    if (!id.contains("gabungan")
                        && !title.contains("gabungan")) continue

                    val score = runCatching {
                        val raw = item.get("score")
                            ?.takeUnless { it.isJsonNull }?.asDouble
                        val maximum = item.get("max_score")
                            ?.takeUnless { it.isJsonNull }?.asDouble
                        PsychotestPolicy.normalizedScore(raw, maximum)
                    }.getOrNull()

                    if (score != null) scores.add(score)
                }
                if (items.size() < 100) break
            }

            val best = scores.maxOrNull()
            result = result.copy(
                psychScore = best,
                psychStatus = if (best == null)
                    "Belum ada hasil Tes Gabungan"
                else
                    "Skor terbaik: $best/100 (Tes Gabungan)"
            )
        } catch (_: Exception) {
            result = result.copy(
                psychStatus = "Gagal membaca riwayat Tes Gabungan"
            )
        }
    }

    return result
}
