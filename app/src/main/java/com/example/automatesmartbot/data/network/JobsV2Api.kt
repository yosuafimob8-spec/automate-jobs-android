package com.example.automatesmartbot.data.network

import com.example.automatesmartbot.data.model.JobsV2Response
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface JobsV2Api {
    @GET("jobs")
    suspend fun searchJobs(
        @Query("positions") positions: List<String>? = null,
        @Query("locations") locations: List<String>? = null,
        @Query("sources") sources: List<String>? = null,
        @Query("categories") categories: List<String>? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("snapshot_id") snapshotId: String? = null
    ): Response<JobsV2Response>
}
