package com.example.automatesmartbot.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MembershipExpiryTest {
    private fun user(pro: Boolean?, membership: String?, expires: String?) =
        UserMeResponse(null, null, null, null, pro, membership, expires)

    @Test fun expiredExplicitProDoesNotUnlock() {
        assertFalse(user(true, "pro", "2000-01-01T00:00:00Z").hasProMembership)
    }

    @Test fun futureExpiryRespectsVerifiedPro() {
        assertTrue(user(true, "free", "2999-01-01T00:00:00Z").hasProMembership)
    }

    @Test fun explicitFreeWinsOverConflictingPlanString() {
        assertFalse(user(false, "pro", "2999-01-01T00:00:00Z").hasProMembership)
    }
}
