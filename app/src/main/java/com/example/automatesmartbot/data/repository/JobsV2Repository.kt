package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.JobsV2Response
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JobsV2Repository(private val sessionManager: SessionManager? = ApiClient.getSessionManager()) {
    suspend fun search(
        positions: List<String>?, locations: List<String>?, sources: List<String>?, categories: List<String>?,
        page: Int, snapshotId: String?
    ): Result<JobsV2Response> = withContext(Dispatchers.IO) {
        try {
            val service = ApiClient.jobsV2Api
                ?: return@withContext Result.failure(Exception("Layanan lowongan V2 belum dikonfigurasi."))
            val response = service.searchJobs(positions, locations, sources, categories, page, 20, snapshotId)
            val body = response.body()
            when {
                response.isSuccessful && body != null && body.ok -> Result.success(body)
                response.code() == 401 -> {
                    sessionManager?.expireSession()
                    Result.failure(Exception("Sesi Anda berakhir. Silakan masuk kembali."))
                }
                response.code() == 410 -> Result.failure(Exception("Hasil pencarian kedaluwarsa. Mulai pencarian lagi."))
                else -> Result.failure(Exception("Pencarian lowongan belum dapat diproses. Coba lagi."))
            }
        } catch (error: Exception) {
            Result.failure(Exception(error.localizedMessage ?: "Tidak dapat terhubung ke layanan lowongan."))
        }
    }
}
