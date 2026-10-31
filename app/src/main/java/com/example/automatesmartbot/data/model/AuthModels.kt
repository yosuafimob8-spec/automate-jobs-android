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
    val membership: String? = null
) {
    val isEmailVerified: Boolean
        get() = when (email_verified) {
            is Boolean -> email_verified
            is Number -> email_verified.toInt() != 0
            is String -> email_verified.toBoolean() || email_verified == "1"
            else -> false
        }

    val hasProMembership: Boolean
        get() = isPro ?: (membership?.equals("pro", ignoreCase = true) == true)

    val hasVerifiedMembership: Boolean
        get() = isPro != null || !membership.isNullOrBlank()
}
