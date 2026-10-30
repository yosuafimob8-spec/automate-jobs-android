package com.example.automatesmartbot.data.network

import com.example.automatesmartbot.data.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.getToken()
        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        val response = chain.proceed(requestBuilder.build())
        if (response.code == 401 && !token.isNullOrBlank()) {
            sessionManager.expireSession()
        }
        return response
    }
}
