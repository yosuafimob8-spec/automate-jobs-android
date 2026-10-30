package com.example.automatesmartbot.data.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PsychotestApi {
    @GET("psychotests/catalog") suspend fun catalog(): Response<JsonObject>
    @POST("psychotests/sessions") suspend fun start(@Body body: JsonObject): Response<JsonObject>
    @GET("psychotests/sessions/{id}/questions") suspend fun questions(@Path("id") id: String): Response<JsonObject>
    @PUT("psychotests/sessions/{id}/answers") suspend fun answers(@Path("id") id: String, @Body body: JsonObject): Response<JsonObject>
    @POST("psychotests/sessions/{id}/submit") suspend fun submit(@Path("id") id: String): Response<JsonObject>
    @GET("psychotests/sessions/{id}/result") suspend fun result(@Path("id") id: String): Response<JsonObject>
    @POST("psychotests/sessions/{id}/retry-wrong") suspend fun retryWrong(@Path("id") id: String): Response<JsonObject>
    @GET("psychotests/history") suspend fun history(@Query("limit") limit: Int = 20, @Query("offset") offset: Int = 0): Response<JsonObject>
    @GET("psychotests/stats") suspend fun stats(): Response<JsonObject>
    @GET("psychotests/leaderboard") suspend fun leaderboard(@Query("board") board: String = "all", @Query("test_id") testId: String? = null): Response<JsonObject>
}
