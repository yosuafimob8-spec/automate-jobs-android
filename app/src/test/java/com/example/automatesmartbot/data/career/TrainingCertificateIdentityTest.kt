package com.example.automatesmartbot.data.career

import org.junit.Assert.*
import org.junit.Test

class TrainingCertificateIdentityTest {
    @Test fun validCertificateEncodesAllFields() {
        val id = "123e4567-e89b-12d3-a456-426614174000"
        val payload = TrainingCertificateIdentity.payload(id, "role_admin_warehouse_basic", "2026-11-02", 100)
        assertEquals("KarirMate|INTERNAL|$id|role_admin_warehouse_basic|2026-11-02|100", payload)
    }

    @Test fun localVerificationMatchesOnlyExistingRecord() {
        val id = "123e4567-e89b-12d3-a456-426614174000"
        val valid = TrainingCertificateIdentity.payload(id, "role_admin_warehouse_basic", "2026-11-02", 100)
        assertTrue(TrainingCertificateIdentity.verifyLocal(valid, listOf(valid)))
        assertFalse(TrainingCertificateIdentity.verifyLocal(valid, emptyList()))
        assertFalse(TrainingCertificateIdentity.verifyLocal(valid.replace("100", "80"), listOf(valid)))
        assertFalse(TrainingCertificateIdentity.verifyLocal("fake", listOf(valid)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCertificateIdRejected() {
        TrainingCertificateIdentity.payload("invalid", "role_admin_basic", "2026-11-02", 80)
    }

    @Test(expected = IllegalArgumentException::class)
    fun impossibleScoreRejected() {
        TrainingCertificateIdentity.payload("123e4567-e89b-12d3-a456-426614174000", "role_admin_basic", "2026-11-02", 101)
    }
}
