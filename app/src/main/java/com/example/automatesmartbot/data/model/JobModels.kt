package com.example.automatesmartbot.data.model

import com.google.gson.annotations.SerializedName

data class JobSearchResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("position") val position: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("total") val total: Int?,
    @SerializedName("jobs") val jobs: List<JobDto>?
)

data class JobDto(
    @SerializedName("title") val title: String?,
    @SerializedName("company") val company: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("position") val position: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("updated") val updated: String?,
    @SerializedName("remote") val remote: Boolean?,
    @SerializedName("match_label") val matchLabel: String?
)
