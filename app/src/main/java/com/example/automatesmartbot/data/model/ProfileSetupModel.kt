package com.example.automatesmartbot.data.model

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("account_id") val accountId: String?,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("whatsapp") val whatsapp: String?,
    @SerializedName("domicile") val domicile: String?,
    @SerializedName("education") val education: String?,
    @SerializedName("desired_positions") val desiredPositions: List<String>?,
    @SerializedName("preferred_locations") val preferredLocations: List<String>?,
    @SerializedName("application_email") val applicationEmail: String?,
    @SerializedName("completed") val completed: Boolean?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class ProfileRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("whatsapp") val whatsapp: String,
    @SerializedName("domicile") val domicile: String,
    @SerializedName("education") val education: String,
    @SerializedName("desiredPositions") val desiredPositions: List<String>,
    @SerializedName("preferredLocations") val preferredLocations: List<String>,
    @SerializedName("applicationEmail") val applicationEmail: String
)

data class ProfileUpdateResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("completed") val completed: Boolean?
)
