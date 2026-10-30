package com.example.automatesmartbot.data.security

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object SessionPolicy {
    const val FALLBACK_LIFETIME_MILLIS: Long = 24L * 60L * 60L * 1000L

    fun parseExpiryMillis(rawValue: String?, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        val raw = rawValue?.trim().orEmpty()
        if (raw.isEmpty()) return null

        raw.toLongOrNull()?.let { numeric ->
            return when {
                numeric <= 0L -> null
                numeric < 100_000_000_000L -> numeric * 1000L
                else -> numeric
            }
        }

        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(raw).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                try {
                    val normalized = raw.replace(' ', 'T')
                    LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }

    fun fallbackExpiryMillis(nowMillis: Long): Long = nowMillis + FALLBACK_LIFETIME_MILLIS

    fun isExpired(expiresAtMillis: Long?, nowMillis: Long): Boolean {
        return expiresAtMillis == null || expiresAtMillis <= nowMillis
    }

    fun isValid(token: String?, expiresAtMillis: Long?, nowMillis: Long): Boolean {
        return !token.isNullOrBlank() && !isExpired(expiresAtMillis, nowMillis)
    }
}
