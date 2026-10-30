package com.example.automatesmartbot.util

import java.net.URI

object UrlSafety {
    /**
     * Returns a trimmed http/https URL only when it has a valid host.
     * Unsupported schemes, blank values, and malformed URLs return null.
     */
    fun safeHttpUrl(raw: String?): String? {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isEmpty()) return null
        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase()
            if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()) {
                candidate
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
