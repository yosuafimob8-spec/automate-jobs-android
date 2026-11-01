package com.example.automatesmartbot.data.interview

import com.example.automatesmartbot.data.autoapply.readJson
import com.example.automatesmartbot.data.cv.CvContent
import com.example.automatesmartbot.data.cv.SharedCvParser
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.*

interface InterviewApi {
    @GET("interviews/capabilities") suspend fun capabilities(): Response<ResponseBody>
    @POST("interviews/sessions") suspend fun start(@Body settings: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody>
    @GET("interviews/sessions/{id}") suspend fun session(@Path("id") id: String): Response<ResponseBody>
    @POST("interviews/sessions/{id}/answers") suspend fun answer(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody>
    @POST("interviews/sessions/{id}/finish") suspend fun finish(@Path("id") id: String): Response<ResponseBody>
    @GET("interviews/history") suspend fun history(): Response<ResponseBody>
    @POST("interviews/sessions/{id}/webrtc") suspend fun offer(@Path("id") id: String, @Body body: Map<String, String>): Response<ResponseBody>
    @POST("interviews/sessions/{id}/transcribe") suspend fun transcribe(@Path("id") id: String, @Body body: Map<String, String>): Response<ResponseBody>
    @POST("interviews/sessions/{id}/speech") suspend fun speech(@Path("id") id: String): Response<ResponseBody>
}

class InterviewRepository(private val api: InterviewApi) {
    suspend fun capabilities() = api.capabilities().readJson()
    suspend fun start(settings: Map<String, Any>): JSONObject {
        val cv = settings["cv"]
        val payload = if (settings["use_backend_cv"] != true && cv is CvContent)
            settings + ("cv" to SharedCvParser.interviewContext(cv)) else settings
        return api.start(payload).readJson().getJSONObject("session")
    }
    suspend fun session(id: String) = api.session(id).readJson().getJSONObject("session")
    suspend fun answer(id: String, body: Map<String, Any>) = api.answer(id, body).readJson().getJSONObject("session")
    suspend fun finish(id: String) = api.finish(id).readJson().getJSONObject("session")
    suspend fun history() = api.history().readJson()
    suspend fun offer(id: String, sdp: String) = api.offer(id, mapOf("type" to "offer", "sdp" to sdp)).readJson()
    suspend fun transcribe(id: String, audio: ByteArray, format: String): String = api.transcribe(id, mapOf("audio_base64" to android.util.Base64.encodeToString(audio, android.util.Base64.NO_WRAP), "audio_format" to format)).readJson().getString("text")
    suspend fun speech(id: String): ByteArray = android.util.Base64.decode(api.speech(id).readJson().getString("audio_base64"), android.util.Base64.DEFAULT)
}
