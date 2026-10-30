package com.example.automatesmartbot.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlSafetyTest {

    @Test
    fun acceptsHttpAndHttpsUrls() {
        assertEquals("https://www.kalibrr.com/c/odoo/jobs/274903", UrlSafety.safeHttpUrl("https://www.kalibrr.com/c/odoo/jobs/274903"))
        assertEquals("http://example.com/jobs", UrlSafety.safeHttpUrl("  http://example.com/jobs  "))
    }

    @Test
    fun rejectsUnsupportedSchemes() {
        assertNull(UrlSafety.safeHttpUrl("javascript:alert(1)"))
        assertNull(UrlSafety.safeHttpUrl("ftp://example.com/file"))
        assertNull(UrlSafety.safeHttpUrl("file:///data/local/tmp/cv.pdf"))
        assertNull(UrlSafety.safeHttpUrl("mailto:hrd@example.com"))
    }

    @Test
    fun rejectsBlankAndMalformedValues() {
        assertNull(UrlSafety.safeHttpUrl(null))
        assertNull(UrlSafety.safeHttpUrl(""))
        assertNull(UrlSafety.safeHttpUrl("   "))
        assertNull(UrlSafety.safeHttpUrl("https://"))
        assertNull(UrlSafety.safeHttpUrl("https://exa mple.com/job"))
    }
}
