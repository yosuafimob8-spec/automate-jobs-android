package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.JobFilters
import com.example.automatesmartbot.data.model.JobSearchResponse
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.security.SessionManager
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class JobRepository(private val sessionManager: SessionManager? = ApiClient.getSessionManager()) {

    suspend fun getJobs(position: String?, location: String?): Result<JobSearchResponse> {
        if (!JobFilters.isValid(position, location)) {
            return Result.failure(IllegalArgumentException(JobFilters.VALIDATION_MESSAGE))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.searchJobs(position, location)
                val body = response.body()
                if (response.isSuccessful && body != null && body.success != false) {
                    Result.success(body)
                } else if (response.isSuccessful) {
                    Result.failure(Exception("Layanan lowongan tidak dapat memenuhi permintaan. Coba lagi."))
                } else if (response.code() == 401) {
                    sessionManager?.expireSession()
                    Result.failure(Exception("Sesi Anda berakhir. Silakan masuk kembali."))
                } else {
                    val serverError = backendErrorMessage(response.errorBody()?.string())
                    Result.failure(
                        Exception(serverError ?: "Gagal mengambil lowongan. Coba lagi.")
                    )
                }
            } catch (e: IOException) {
                Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet Anda."))
            } catch (e: HttpException) {
                if (e.code() == 401) sessionManager?.expireSession()
                Result.failure(Exception("Gagal memuat lowongan. Coba lagi."))
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "Gagal memuat lowongan"))
            }
        }
    }

    private fun backendErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return runCatching {
            JsonParser.parseString(rawBody)
                .asJsonObject
                .get("error")
                ?.takeIf { !it.isJsonNull }
                ?.asString
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
