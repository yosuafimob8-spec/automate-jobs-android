package com.example.automatesmartbot.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun saveToken(token: String, expiresAt: String?) {
        val now = System.currentTimeMillis()
        val expiryMillis = SessionPolicy.parseExpiryMillis(expiresAt)
            ?: SessionPolicy.fallbackExpiryMillis(now)
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_EXPIRES_AT, expiresAt)
            .putLong(KEY_EXPIRES_AT_MILLIS, expiryMillis)
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getExpiresAt(): String? {
        return sharedPreferences.getString(KEY_EXPIRES_AT, null)
    }

    fun getValidToken(): String? {
        val token = getToken()
        val expiryMillis = getExpiryMillis()
        val now = System.currentTimeMillis()
        if (!SessionPolicy.isValid(token, expiryMillis, now)) {
            clearSession()
            return null
        }
        return token
    }

    fun hasValidSession(): Boolean = getValidToken() != null

    fun isProfileCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_PROFILE_COMPLETED, false)
    }

    fun setProfileCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PROFILE_COMPLETED, completed).apply()
    }

    fun expireSession() {
        val hadToken = !getToken().isNullOrBlank()
        clearSession()
        if (hadToken) {
            _sessionExpired.tryEmit(Unit)
        }
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_EXPIRES_AT_MILLIS)
            .remove(KEY_PROFILE_COMPLETED)
            .apply()
    }

    private fun getExpiryMillis(): Long? {
        if (!sharedPreferences.contains(KEY_EXPIRES_AT_MILLIS)) {
            val migrated = SessionPolicy.parseExpiryMillis(getExpiresAt())
            if (migrated != null) {
                sharedPreferences.edit().putLong(KEY_EXPIRES_AT_MILLIS, migrated).apply()
            }
            return migrated
        }
        return sharedPreferences.getLong(KEY_EXPIRES_AT_MILLIS, 0L).takeIf { it > 0L }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_EXPIRES_AT_MILLIS = "expires_at_millis"
        private const val KEY_PROFILE_COMPLETED = "profile_completed"
    }
}
