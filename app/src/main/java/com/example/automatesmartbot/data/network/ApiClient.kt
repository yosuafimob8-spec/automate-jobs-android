package com.example.automatesmartbot.data.network

import android.content.Context
import com.example.automatesmartbot.BuildConfig
import com.example.automatesmartbot.data.interview.InterviewApi
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
            jobsV2Api = BuildConfig.JOBS_V2_BASE_URL.takeIf { it.isNotBlank() }
                ?.let { createJobsV2Api(sessionManager!!) }
            psychotestApi = createPsychotestApi(sessionManager!!)
            interviewApi = createInterviewApi(sessionManager!!)
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

    private fun createInterviewApi(sm: SessionManager): InterviewApi {
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(sm)).connectTimeout(10, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
        return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(InterviewApi::class.java)
    }

    private fun createJobsV2Api(sm: SessionManager): JobsV2Api {
        val url = BuildConfig.JOBS_V2_BASE_URL
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sm))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(210, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder().baseUrl(url).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(JobsV2Api::class.java)
    }

    private fun createPsychotestApi(sm: SessionManager): PsychotestApi? {
        if (BuildConfig.PSYCHOTEST_BASE_URL.isBlank()) return null
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(sm)).connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
        return Retrofit.Builder().baseUrl(BuildConfig.PSYCHOTEST_BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(PsychotestApi::class.java)
    }

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    var apiService: ApiService = retrofit.create(ApiService::class.java)
        private set

    var jobsV2Api: JobsV2Api? = null
        private set
    var interviewApi: InterviewApi? = null
        private set
    var psychotestApi: PsychotestApi? = null
        private set
}
