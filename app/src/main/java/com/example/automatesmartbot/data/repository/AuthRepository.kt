package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.*
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val sessionManager: SessionManager? = ApiClient.getSessionManager()) {

    suspend fun register(request: RegisterRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.register(request)
                if (response.code() == 201 || response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Registrasi gagal (${response.code()}): Email mungkin sudah terdaftar."))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet Anda."))
            } catch (e: HttpException) {
                Result.failure(Exception("Registrasi gagal. Coba lagi."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.login(request)
                if (response.isSuccessful && response.body() != null) {
                    val loginRes = response.body()!!
                    // Save token securely using EncryptedSharedPreferences (never logged)
                    sessionManager?.saveToken(loginRes.access_token, loginRes.expires_at)
                    Result.success(loginRes)
                } else {
                    Result.failure(Exception("Login gagal (${response.code()}): Email atau password salah."))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Tidak dapat terhubung. Periksa koneksi internet Anda."))
            } catch (e: HttpException) {
                Result.failure(Exception("Login gagal. Coba lagi."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getMe(): Result<UserMeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getMe()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
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

    fun logout() {
        sessionManager?.clearSession()
    }
}
