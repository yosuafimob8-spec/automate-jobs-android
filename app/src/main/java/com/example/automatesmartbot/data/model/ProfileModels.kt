package com.example.automatesmartbot.data.model

data class UserProfile(
    val id: String?,
    val username: String?,
    val email: String?,
    val membershipType: String?,
    val isPro: Boolean
)

data class UserPreferences(
    val themeDark: Boolean,
    val notificationsEnabled: Boolean,
    val autoSyncServer: Boolean
)

data class UserQuota(
    val remainingJobQuota: Int,
    val remainingAutoApplyQuota: Int,
    val maxJobQuota: Int
)
