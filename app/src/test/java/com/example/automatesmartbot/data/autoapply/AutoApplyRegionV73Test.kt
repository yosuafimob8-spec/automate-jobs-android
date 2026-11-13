package com.example.automatesmartbot.data.autoapply

import org.junit.Assert.*
import org.junit.Test

class AutoApplyRegionV73Test {
    @Test fun positionIsFormalAndBekasiIsMatched() {
        assertTrue(AutoApplyTargetPolicy.matchesCandidate(
            "Operator Produksi", "Kota Bekasi", setOf("Sales"), setOf("Bekasi")))
    }

    @Test fun dontCrossRegions() {
        assertFalse(AutoApplyTargetPolicy.matchesCandidate(
            "Sales", "Bogor", setOf("Sales"), setOf("Bekasi")))
        assertFalse(AutoApplyTargetPolicy.matchesCandidate(
            "Sales", "Tangerang Selatan", setOf("Sales"), setOf("Tangerang")))
        assertTrue(AutoApplyTargetPolicy.matchesCandidate(
            "Sales", "Cikarang", setOf("Sales"), setOf("Bekasi")))
    }

    @Test fun newWestJavaLocationsAreSelectable() {
        assertTrue(AutoApplyTargetPolicy.regions.contains("Bandung"))
        assertTrue(AutoApplyTargetPolicy.regions.contains("Purwakarta"))
    }
}
