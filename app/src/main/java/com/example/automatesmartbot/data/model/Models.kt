package com.example.automatesmartbot.data.model

data class DashboardStatusResponse(
    val status: String,
    val isPro: Boolean,
    val membershipVerified: Boolean = false,
    val remainingJobQuota: Int? = null,
    val remainingAutoApplyQuota: Int? = null,
    val botStatus: String,
    val isOnline: Boolean = false,
    val emailVerified: Boolean? = null,
    val profileCompleted: Boolean? = null,
    val displayName: String? = null,
    val jobCount: Int? = null
)

data class HealthResponse(
    val status: String?,
    val message: String?
)

data class JobModel(
    val id: String?,
    val title: String,
    val company: String,
    val location: String,
    val salary: String,
    val matchScore: Int,
    val platform: String
)

data class EmailApplyRequest(
    val targetEmail: String,
    val position: String,
    val coverLetter: String
)

data class EmailApplyResponse(
    val success: Boolean,
    val message: String
)

data class ActivityItemModel(
    val id: String?,
    val title: String,
    val description: String,
    val time: String,
    val status: String // "Berhasil" atau "Gagal"
)

data class UserProfileResponse(
    val username: String,
    val email: String,
    val isPro: Boolean,
    val appVersion: String,
    val backendStatus: String
)
