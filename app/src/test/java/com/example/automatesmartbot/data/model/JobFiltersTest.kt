package com.example.automatesmartbot.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobFiltersTest {

    @Test
    fun supportedFiltersNeverContainSemuaValues() {
        assertFalse(JobFilters.positions.contains("Semua Posisi"))
        assertFalse(JobFilters.locations.contains("Semua Lokasi"))
    }

    @Test
    fun everySupportedCombinationIsValid() {
        JobFilters.positions.forEach { position ->
            JobFilters.locations.forEach { location ->
                assertTrue("$position / $location", JobFilters.isValid(position, location))
            }
        }
    }

    @Test
    fun missingOrUnsupportedFiltersAreRejected() {
        assertFalse(JobFilters.isValid(null, "Tangerang"))
        assertFalse(JobFilters.isValid("Admin Warehouse", null))
        assertFalse(JobFilters.isValid("", "Tangerang"))
        assertFalse(JobFilters.isValid("Semua Posisi", "Tangerang"))
        assertFalse(JobFilters.isValid("Admin Warehouse", "Semua Lokasi"))
        assertFalse(JobFilters.isValid("Unknown", "Tangerang"))
        assertFalse(JobFilters.isValid("Admin Warehouse", "Bandung"))
    }

    @Test
    fun supportedListsMatchConfirmedBackendValues() {
        assertEquals(7, JobFilters.positions.size)
        assertEquals(5, JobFilters.locations.size)
        assertEquals("Admin Warehouse", JobFilters.positions.first())
        assertEquals("Tangerang", JobFilters.locations.first())
    }
}
