package com.example.automatesmartbot.data.autoapply

import org.junit.Assert.*
import org.junit.Test

/** Add to app/src/test/... after the source patch if wanted. */
class AutoApplyRegionRegressionTest {
    @Test fun tangselIsNotIncludedInTangerang() {
        val position = setOf("Staff Logistik")
        assertFalse(AutoApplyTargetPolicy.matchesCandidate(
            "Staff Logistik", "Tangerang Selatan", position, setOf("Tangerang")))
        assertFalse(AutoApplyTargetPolicy.matchesCandidate(
            "Staff Logistik", "BSD Serpong", position, setOf("Tangerang")))
        assertTrue(AutoApplyTargetPolicy.matchesCandidate(
            "Staff Logistik", "Tangerang Selatan", position, setOf("Tangerang Selatan")))
        assertTrue(AutoApplyTargetPolicy.matchesCandidate(
            "Staff Logistik", "Karawaci Tangerang", position, setOf("Tangerang")))
    }
    @Test fun existingBekasiCikarangAliasIsPreserved() {
        assertTrue(AutoApplyTargetPolicy.matchesCandidate(
            "Admin Logistik", "Cikarang", setOf("Admin Logistik"), setOf("Bekasi")))
    }
}
