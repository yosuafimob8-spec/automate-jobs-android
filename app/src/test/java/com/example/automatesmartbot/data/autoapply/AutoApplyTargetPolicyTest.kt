package com.example.automatesmartbot.data.autoapply

import org.junit.Assert.*
import org.junit.Test

class AutoApplyTargetPolicyTest {
    @Test fun requiresBothPositionAndRegion() {
        assertFalse(AutoApplyTargetPolicy.matchesCandidate("Staff Gudang", "Bogor", emptySet(), setOf("Bogor")))
        assertFalse(AutoApplyTargetPolicy.matchesCandidate("Staff Gudang", "", setOf("Staff Warehouse"), setOf("Bogor")))
    }
    @Test fun matchesMultipleLocationsAndWarehouseSynonyms() {
        assertTrue(AutoApplyTargetPolicy.matchesCandidate("Staff Gudang", "Bogor Barat", setOf("Staff Warehouse"), setOf("Bogor", "Depok")))
        assertTrue(AutoApplyTargetPolicy.matchesCandidate("Admin Logistik", "Cikarang", setOf("Admin Logistik"), setOf("Bekasi")))
    }
    @Test fun rejectsUnrelatedAndUnknownLocation() {
        assertFalse(AutoApplyTargetPolicy.matchesCandidate("Sales", "Bogor", setOf("Staff Warehouse"), setOf("Bogor")))
        assertFalse(AutoApplyTargetPolicy.matchesCandidate("Staff Warehouse", "", setOf("Staff Warehouse"), setOf("Bogor")))
    }
}
