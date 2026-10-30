package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.*
import com.example.automatesmartbot.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

sealed interface ProfileResult {
    data class Success(val profile: UserProfile, val quota: UserQuota, val preferences: UserPreferences) : ProfileResult
    object Unauthorized : ProfileResult
    object Offline : ProfileResult
    data class Error(val message: String) : ProfileResult
}

class ProfileRepository {
    suspend fun fetchProfile(): ProfileResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val profile = UserProfile(
                        id = null,
                        username = body.username,
                        email = body.email,
                        membershipType = if (body.isPro) "PRO" else "FREE",
                        isPro = body.isPro
                    )
                    val quota = UserQuota(
                        remainingJobQuota = if (body.isPro) 999 else 25,
                        remainingAutoApplyQuota = if (body.isPro) 999 else 3,
                        maxJobQuota = if (body.isPro) 9999 else 30
                    )
                    val preferences = UserPreferences(
                        themeDark = true,
                        notificationsEnabled = true,
                        autoSyncServer = true
                    )
                    ProfileResult.Success(profile, quota, preferences)
                } else if (response.code() == 401) {
                    ProfileResult.Unauthorized
                } else {
                    ProfileResult.Error("Gagal memuat profil. Coba lagi.")
                }
            } catch (e: IOException) {
                ProfileResult.Offline
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    ProfileResult.Unauthorized
                } else {
                    ProfileResult.Error(e.message() ?: "Unknown error")
                }
            } catch (e: Exception) {
                ProfileResult.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }
}

