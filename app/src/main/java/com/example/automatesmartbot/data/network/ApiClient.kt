package com.example.automatesmartbot.data.network

import android.content.Context
import com.example.automatesmartbot.BuildConfig
import com.example.automatesmartbot.data.security.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var baseUrl: String = BuildConfig.BASE_URL
    private var sessionManager: SessionManager? = null

    fun init(context: Context) {
        if (sessionManager == null) {
            sessionManager = SessionManager(context.applicationContext)
            retrofit = createRetrofit(baseUrl, sessionManager!!)
            apiService = retrofit.create(ApiService::class.java)
        }
    }

    fun getBaseUrl(): String = baseUrl

    fun getSessionManager(): SessionManager? = sessionManager

    private fun createRetrofit(url: String, sm: SessionManager): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sm))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    var apiService: ApiService = retrofit.create(ApiService::class.java)
        private set
}
