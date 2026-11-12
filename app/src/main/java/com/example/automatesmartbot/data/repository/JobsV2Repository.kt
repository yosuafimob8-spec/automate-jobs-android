package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.JobsV2Response
import com.example.automatesmartbot.data.model.JobsV2Job
import com.example.automatesmartbot.data.model.JobsV2Pagination
import com.example.automatesmartbot.data.model.JobsV2Cache
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JobsV2Repository(private val sessionManager: SessionManager? = ApiClient.getSessionManager()) {
    /** Read-only fallback to the authenticated local /jobs endpoint.
     * Uses existing user-selected filters; does not invent vacancies or send emails.
     * Intentionally limited to four queries so an offline backend cannot hang the app.
     */
    private suspend fun searchLegacyWhenV2Unavailable(
        positions: List<String>?, locations: List<String>?, page: Int
    ): Result<JobsV2Response> {
        if (page != 1 || positions.isNullOrEmpty() || locations.isNullOrEmpty()) {
            return Result.failure(Exception(
                "Layanan lowongan V2 belum tersedia. Pilih posisi dan lokasi lalu tekan Cari."
            ))
        }
        val result = linkedMapOf<String, JobsV2Job>()
        var reachable = false
        for (position in positions.distinct().take(2)) {
            for (location in locations.distinct().take(2)) {
                try {
                    val response = ApiClient.apiService.searchJobs(position, location)
                    if (response.code() == 401) {
                        sessionManager?.expireSession()
                        return Result.failure(Exception("Sesi berakhir. Masuk kembali."))
                    }
                    val payload = response.body()
                    if (response.isSuccessful && payload?.success == true) {
                        reachable = true
                        payload.jobs.orEmpty().forEach { job ->
                            val link = job.link.orEmpty()
                            if (!link.startsWith("https://")) return@forEach
                            result.putIfAbsent(link, JobsV2Job(
                                title = job.title,
                                company = job.company,
                                location = job.location,
                                link = link,
                                source = job.source,
                                sourceId = null,
                                publishedAt = null,
                                resultType = "external",
                                opportunityLabel = job.matchLabel,
                                vacancyVerified = null,
                                applicationActions = emptyList()
                            ))
                        }
                    }
                } catch (_: Exception) {
                    // Try the next user-selected filter; no email or scheduling action.
                }
            }
        }
        if (!reachable) return Result.failure(Exception(
            "Server pencarian lowongan V2 dan cadangan tidak dapat dihubungi. Periksa port 8788/8787."
        ))
        return Result.success(JobsV2Response(
            ok = true, jobs = result.values.toList(), total = result.size,
            partial = true,
            emptyReason = if (result.isEmpty()) "Tidak ada hasil pada data lowongan lokal." else null,
            pagination = JobsV2Pagination(page = 1, hasMore = false, nextPage = null),
            cache = JobsV2Cache(snapshotId = null)
        ))
    }

    suspend fun search(
        positions: List<String>?, locations: List<String>?, sources: List<String>?, categories: List<String>?,
        page: Int, snapshotId: String?
    ): Result<JobsV2Response> = withContext(Dispatchers.IO) {
        try {
            val service = ApiClient.jobsV2Api
                ?: return@withContext searchLegacyWhenV2Unavailable(positions, locations, page)
            val response = service.searchJobs(positions, locations, sources, categories, page, 20, snapshotId)
            val body = response.body()
            when {
                response.isSuccessful && body != null && body.ok -> Result.success(body)
                response.code() == 401 -> {
                    sessionManager?.expireSession()
                    Result.failure(Exception("Sesi Anda berakhir. Silakan masuk kembali."))
                }
                response.code() == 410 -> Result.failure(Exception("Hasil pencarian kedaluwarsa. Mulai pencarian lagi."))
                response.code() == 404 || response.code() >= 500 ->
                    searchLegacyWhenV2Unavailable(positions, locations, page)
                else -> Result.failure(Exception("Pencarian lowongan ditolak (HTTP ${response.code()})."))
            }
        } catch (error: Exception) {
            val fallback = searchLegacyWhenV2Unavailable(positions, locations, page)
            if (fallback.isSuccess) fallback else Result.failure(Exception(
                fallback.exceptionOrNull()?.localizedMessage ?: "Layanan lowongan tidak tersedia."
            ))
        }
    }
}
