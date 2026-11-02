package com.example.automatesmartbot.data.career

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Authenticated training API client contract. Supply the existing login session token
 * from the application's auth layer; never embed a registry admin token in Android.
 * Not wired into JobTrainingScreen until the backend session verifier is available.
 */
class TrainingServerGateway(private val baseUrl: String, private val accessToken: () -> String?) {
    init {
        require(baseUrl.startsWith("https://") && !baseUrl.contains("?") && !baseUrl.contains("#")) {
            "Training server requires HTTPS"
        }
    }

    private fun request(path: String, payload: JSONObject? = null): JSONObject {
        val token = accessToken()?.takeIf { it.isNotBlank() }
            ?: error("Login diperlukan sebelum mengakses pelatihan online")
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (payload == null) "GET" else "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                error("Training server HTTP ${connection.responseCode}: $body")
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    fun questions(courseId: String): JSONObject {
        require(courseId.matches(Regex("[a-z0-9_]+")))
        return request("/training/courses/$courseId/questions")
    }

    fun submitExam(courseId: String, answers: List<Int>): JSONObject {
        require(courseId.matches(Regex("[a-z0-9_]+")))
        require(answers.isNotEmpty() && answers.all { it >= 0 })
        return request("/training/exams", JSONObject()
            .put("courseId", courseId)
            .put("answers", JSONArray(answers)))
    }

    fun certificateStatus(id: String): JSONObject {
        require(id.matches(Regex("[a-fA-F0-9-]{36}")))
        return request("/verify?id=$id")
    }
}
