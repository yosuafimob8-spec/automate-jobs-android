package com.example.automatesmartbot.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Local model storage. Does not alter the existing interview backend. */
class OfflineModelManager(context: Context) {
    private val modelDir = File(context.applicationContext.filesDir, "offline_ai/models").apply { mkdirs() }

    fun modelFile(name: String): File {
        require(name.matches(Regex("[A-Za-z0-9._-]+\\.gguf"))) { "Invalid model filename" }
        return File(modelDir, name)
    }

    fun installed(name: String, expectedSha256: String? = null): Boolean {
        val file = modelFile(name)
        return file.isFile && file.length() > 0 &&
            (expectedSha256 == null || digest(file).equals(expectedSha256, ignoreCase = true))
    }

    suspend fun download(
        httpsUrl: String,
        name: String,
        expectedSha256: String,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        require(httpsUrl.startsWith("https://")) { "Model URL must use HTTPS" }
        require(expectedSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "SHA-256 required" }
        val target = modelFile(name)
        val partial = File(modelDir, "$name.part")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(httpsUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = false
            }
            require(connection.responseCode == 200) { "Download failed: HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong
            var received = 0L
            connection.inputStream.use { input ->
                FileOutputStream(partial, false).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        received += count
                        onProgress(received, total)
                    }
                    output.fd.sync()
                }
            }
            require(received > 0 && (total <= 0 || received == total)) { "Incomplete model download" }
            require(digest(partial).equals(expectedSha256, ignoreCase = true)) { "Model checksum mismatch" }
            if (target.exists()) require(target.delete()) { "Cannot replace model" }
            require(partial.renameTo(target)) { "Cannot install model" }
            target
        } finally {
            connection?.disconnect()
            if (partial.exists()) partial.delete()
        }
    }

    fun remove(name: String): Boolean = modelFile(name).let { !it.exists() || it.delete() }

    private fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
