package com.example.automatesmartbot.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String?,
    val expires_at: String?,
    val email_verified: Any?
) {
    val isEmailVerified: Boolean
        get() = when (email_verified) {
            is Boolean -> email_verified
            is Number -> email_verified.toInt() != 0
            is String -> email_verified.toBoolean() || email_verified == "1"
            else -> false
        }
}

data class UserMeResponse(
    val id: String?,
    val email: String?,
    val email_verified: Any?,
    val telegram_user_id: String?,
    @SerializedName("is_pro") val isPro: Boolean? = null,
    val membership: String? = null,
    @SerializedName("membership_expires_at") val membershipExpiresAt: String? = null
) {
    val isEmailVerified: Boolean
        get() = when (email_verified) {
            is Boolean -> email_verified
            is Number -> email_verified.toInt() != 0
            is String -> email_verified.toBoolean() || email_verified == "1"
            else -> false
        }

    /** The backend is authoritative; a parseable expired date can never unlock PRO. */
    val hasProMembership: Boolean
        get() {
            val serverGrantsPro = isPro ?: (membership?.equals("pro", ignoreCase = true) == true)
            if (!serverGrantsPro) return false
            val expiry = membershipExpiresAt?.takeIf { it.isNotBlank() }?.let { value ->
                runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrNull()
                    ?: runCatching { java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            }
            return expiry == null || expiry > System.currentTimeMillis()
        }

    val hasVerifiedMembership: Boolean
        get() = isPro != null || !membership.isNullOrBlank()
}
