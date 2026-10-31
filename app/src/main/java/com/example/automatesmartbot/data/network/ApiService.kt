package com.example.automatesmartbot.data.network

import com.example.automatesmartbot.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

data class AutoApplyEnableRequest(
    val consent: Boolean,
    val max_emails_per_day: Int = 5,
    val policy_version: String = "karirmate-v1"
)

interface ApiService {
    @POST("gmail/oauth/start")
    suspend fun startGmailOAuth(@Body request: Map<String, String> = emptyMap()): Response<ResponseBody>
    @PUT("cv")
    suspend fun uploadAutoApplyCv(@Body pdf: okhttp3.RequestBody): Response<ResponseBody>

    @GET("karirmate/candidates")
    suspend fun getKarirMateCandidates(): Response<ResponseBody>

    @GET("karirmate/selection")
    suspend fun getKarirMateSelection(): Response<ResponseBody>

    @GET("karirmate/report")
    suspend fun getKarirMateReport(@Query("period") period: String? = null, @Query("offset") offset: Int? = null): Response<ResponseBody>

    @POST("auto-apply/simulate")
    suspend fun simulateAutoApply(@Body request: Map<String, String> = emptyMap()): Response<ResponseBody>

    @POST("auto-apply/enable")
    suspend fun enableAutoApply(@Body request: AutoApplyEnableRequest): Response<ResponseBody>

    @POST("auto-apply/pause")
    suspend fun pauseAutoApply(): Response<ResponseBody>
    @GET("auto-apply/status")
    suspend fun getAutoApplyStatus(): Response<ResponseBody>
    @GET("health")
    suspend fun getHealth(): Response<ResponseBody>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserMeResponse>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("profile")
    suspend fun updateProfile(@Body profile: ProfileRequest): Response<ProfileUpdateResponse>

    @GET("jobs")
    suspend fun searchJobs(
        @Query("position") position: String?,
        @Query("location") location: String?
    ): Response<JobSearchResponse>

    @GET("api/jobs")
    suspend fun getJobsOld(@Query("query") query: String? = null): Response<List<JobModel>>

    @POST("api/apply/job")
    suspend fun applyJob(@Body jobId: Map<String, String>): Response<EmailApplyResponse>

    @POST("api/apply/email")
    suspend fun sendEmailApplication(@Body request: EmailApplyRequest): Response<EmailApplyResponse>

    @GET("api/activities")
    suspend fun getActivities(): Response<List<ActivityItemModel>>

    @GET("api/profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>
}
