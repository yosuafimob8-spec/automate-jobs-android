package com.example.automatesmartbot.data.cv

import org.junit.Assert.*
import org.junit.Test

class CvTemplatesAccessTest {
    @Test fun catalogHasExpectedTierCounts() {
        assertEquals(75, CvTemplates.catalog.size)
        assertEquals(75, CvTemplates.catalog.map { it.id }.toSet().size)
        assertEquals(27, CvTemplates.freeIds.size)
        assertEquals(48, CvTemplates.catalog.count { CvTemplates.isProTemplate(it.id) })
    }

    @Test fun originalTemplatesStayFree() {
        CvTemplates.all.forEach { assertTrue(CvTemplates.canUse(it.id, false)) }
    }

    @Test fun premiumTemplatesRequireVerifiedAccess() {
        CvTemplates.catalog.filter { CvTemplates.isProTemplate(it.id) }.forEach {
            assertFalse(CvTemplates.canUse(it.id, false))
            assertTrue(CvTemplates.canUse(it.id, true))
        }
    }

    @Test fun legacyDraftsRemainReadable() {
        listOf("student", "combination", "senior").forEach {
            assertTrue(CvTemplates.canUse(it, false))
            assertNotNull(CvTemplates.resolve(it))
        }
    }

    @Test fun unknownTemplateCannotBeUsed() {
        assertFalse(CvTemplates.canUse("unknown_template", false))
        assertFalse(CvTemplates.canUse("unknown_template", true))
    }
}
