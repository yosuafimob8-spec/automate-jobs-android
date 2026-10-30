package com.example.automatesmartbot.data.model

import com.google.gson.annotations.SerializedName

data class JobsV2Response(
    val ok: Boolean,
    val jobs: List<JobsV2Job> = emptyList(),
    val total: Int = 0,
    val partial: Boolean = false,
    @SerializedName("empty_reason") val emptyReason: String? = null,
    val pagination: JobsV2Pagination,
    val cache: JobsV2Cache
)

data class JobsV2Job(
    val title: String?,
    val company: String?,
    val location: String?,
    val link: String?,
    val source: String?,
    @SerializedName("source_id") val sourceId: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("result_type") val resultType: String? = null,
    @SerializedName("opportunity_label") val opportunityLabel: String? = null,
    @SerializedName("vacancy_verified") val vacancyVerified: Boolean? = null,
    @SerializedName("application_actions") val applicationActions: List<ApplicationAction> = emptyList()
)

data class ApplicationAction(val method: String?, val label: String?, val url: String?)

data class JobsV2Pagination(
    val page: Int?,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("next_page") val nextPage: Int?
)

data class JobsV2Cache(@SerializedName("snapshot_id") val snapshotId: String?)
