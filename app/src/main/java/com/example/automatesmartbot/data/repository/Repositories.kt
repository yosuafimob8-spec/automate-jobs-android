package com.example.automatesmartbot.data.repository

import android.util.Log
import com.example.automatesmartbot.data.model.*
import com.example.automatesmartbot.data.network.ApiClient
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class DashboardRepository {
    suspend fun getDashboardStatus(): Result<DashboardStatusResponse> {
        return withContext(Dispatchers.IO) {
            val healthResponse = try {
                ApiClient.apiService.getHealth()
            } catch (e: Exception) {
                Log.e("ApiClient", "Health check failed", e)
                null
            }

            val healthJson = healthResponse
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.let { body -> runCatching { JsonParser.parseString(body.string()).asJsonObject }.getOrNull() }

            val online = healthJson
                ?.get("status")
                ?.takeIf { !it.isJsonNull }
                ?.asString
                ?.equals("online", ignoreCase = true) == true

            if (!online) {
                return@withContext Result.success(
                    DashboardStatusResponse(
                        status = "Offline",
                        isPro = false,
                        membershipVerified = false,
                        remainingJobQuota = null,
                        remainingAutoApplyQuota = null,
                        botStatus = "Offline",
                        isOnline = false
                    )
                )
            }

            val meResponse = runCatching { ApiClient.apiService.getMe() }.getOrNull()
            val profileResponse = runCatching { ApiClient.apiService.getProfile() }.getOrNull()
            val jobsResponse = runCatching {
                ApiClient.apiService.searchJobs(
                    JobFilters.positions.first(),
                    JobFilters.locations.first()
                )
            }.getOrNull()
            val me = meResponse?.takeIf { it.isSuccessful }?.body()
            val profile = profileResponse?.takeIf { it.isSuccessful }?.body()
            val jobCount = jobsResponse
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.takeIf { it.success == true }
                ?.total

            Result.success(
                DashboardStatusResponse(
                    status = "Online",
                    isPro = me?.hasProMembership == true,
                    membershipVerified = me?.hasVerifiedMembership == true,
                    remainingJobQuota = null,
                    remainingAutoApplyQuota = null,
                    botStatus = "Online",
                    isOnline = true,
                    emailVerified = me?.isEmailVerified,
                    profileCompleted = profile?.completed,
                    displayName = profile?.fullName,
                    jobCount = jobCount
                )
            )
        }
    }
}

class EmailRepository {
    suspend fun sendEmail(request: EmailApplyRequest): Result<EmailApplyResponse> {
        return withContext(Dispatchers.IO) {
            Result.failure(
                IllegalStateException(
                    "Pengiriman lamaran belum tersedia."
                )
            )
        }
    }
}

class ActivityRepository {
    suspend fun getActivities(): Result<List<ActivityItemModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getActivities()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else if (response.code() == 401) {
                    Result.failure(Exception("Sesi habis (401 Unauthorized). Silakan login kembali."))
                } else {
                    Result.failure(Exception("Gagal memuat riwayat aktivitas (${response.code()})"))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet Anda."))
            } catch (e: HttpException) {
                Result.failure(Exception("Gagal memuat riwayat aktivitas. Coba lagi."))
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "Gagal memuat riwayat aktivitas"))
            }
        }
    }
}

class UserRepository {
    suspend fun getUserProfile(isProLocal: Boolean): Result<UserProfileResponse> {
        return withContext(Dispatchers.IO) {
            var apiHealthy = false
            try {
                val healthResponse = ApiClient.apiService.getHealth()
                apiHealthy = healthResponse.isSuccessful
            } catch (e: Exception) {
                apiHealthy = false
            }

            try {
                val response = ApiClient.apiService.getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Result.success(
                        body.copy(
                            backendStatus = if (apiHealthy) "Online" else "Offline"
                        )
                    )
                } else {
                    Result.success(
                        UserProfileResponse(
                            username = "",
                            email = "",
                            isPro = false,
                            appVersion = "1.0.0",
                            backendStatus = if (apiHealthy) "Online" else "Offline"
                        )
                    )
                }
            } catch (e: Exception) {
                Result.success(
                    UserProfileResponse(
                        username = "",
                        email = "",
                        isPro = false,
                        appVersion = "1.0.0",
                        backendStatus = if (apiHealthy) "Online" else "Offline"
                    )
                )
            }
        }
    }
}
