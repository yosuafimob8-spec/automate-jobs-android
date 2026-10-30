package com.example.automatesmartbot.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class SessionPolicyTest {

    @Test
    fun parsesIsoExpiry() {
        val raw = "2026-10-30T10:15:30Z"
        assertEquals(
            Instant.parse(raw).toEpochMilli(),
            SessionPolicy.parseExpiryMillis(raw, ZoneOffset.UTC)
        )
    }

    @Test
    fun parsesEpochSecondsAndMillis() {
        assertEquals(1_800_000_000_000L, SessionPolicy.parseExpiryMillis("1800000000"))
        assertEquals(1_800_000_000_000L, SessionPolicy.parseExpiryMillis("1800000000000"))
    }

    @Test
    fun rejectsMissingAndMalformedExpiry() {
        assertNull(SessionPolicy.parseExpiryMillis(null))
        assertNull(SessionPolicy.parseExpiryMillis(""))
        assertNull(SessionPolicy.parseExpiryMillis("not-a-date"))
        assertNull(SessionPolicy.parseExpiryMillis("0"))
    }

    @Test
    fun expiredAndMissingSessionsAreNotValid() {
        val now = 1_800_000_000_000L
        assertFalse(SessionPolicy.isValid("token", now - 1L, now))
        assertFalse(SessionPolicy.isValid("token", now, now))
        assertFalse(SessionPolicy.isValid("token", null, now))
        assertFalse(SessionPolicy.isValid(null, now + 60_000L, now))
        assertFalse(SessionPolicy.isValid("", now + 60_000L, now))
    }

    @Test
    fun futureSessionIsValid() {
        val now = 1_800_000_000_000L
        assertTrue(SessionPolicy.isValid("token", now + 1L, now))
    }

    @Test
    fun missingTokenAfterLogoutIsInvalid() {
        val now = 1_800_000_000_000L
        assertFalse(SessionPolicy.isValid(null, now + SessionPolicy.FALLBACK_LIFETIME_MILLIS, now))
    }

    @Test
    fun fallbackExpiryIsExactlyTwentyFourHours() {
        val now = 1_800_000_000_000L
        assertEquals(now + 86_400_000L, SessionPolicy.fallbackExpiryMillis(now))
    }
}
