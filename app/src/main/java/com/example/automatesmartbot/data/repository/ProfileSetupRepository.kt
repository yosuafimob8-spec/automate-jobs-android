package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.ProfileRequest
import com.example.automatesmartbot.data.model.ProfileResponse
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ProfileSetupRepository(private val sessionManager: SessionManager? = ApiClient.getSessionManager()) {

    suspend fun fetchProfile(): Result<ProfileResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.completed == true) {
                        sessionManager?.setProfileCompleted(true)
                    } else {
                        sessionManager?.setProfileCompleted(false)
                    }
                    Result.success(body)
                } else if (response.code() == 401) {
                    sessionManager?.expireSession()
                    Result.failure(Exception("Sesi Anda berakhir. Silakan masuk kembali."))
                } else {
                    Result.failure(Exception("Gagal memuat profil. Coba lagi."))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet Anda."))
            } catch (e: HttpException) {
                if (e.code() == 401) sessionManager?.expireSession()
                Result.failure(Exception("Gagal memuat profil. Coba lagi."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun saveProfile(request: ProfileRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.updateProfile(request)
                if (response.isSuccessful && ProfileSavePolicy.isConfirmed(response.body())) {
                    sessionManager?.setProfileCompleted(true)
                    Result.success(Unit)
                } else if (response.code() == 401) {
                    sessionManager?.expireSession()
                    Result.failure(Exception("Sesi Anda berakhir. Silakan masuk kembali."))
                } else {
                    Result.failure(Exception("Gagal menyimpan profil. Coba lagi."))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet Anda."))
            } catch (e: HttpException) {
                if (e.code() == 401) sessionManager?.expireSession()
                Result.failure(Exception("Gagal menyimpan profil. Coba lagi."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun checkServerProfileCompletion(): Boolean? {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getProfile()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val isComplete = body.completed == true
                    sessionManager?.setProfileCompleted(isComplete)
                    isComplete
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
