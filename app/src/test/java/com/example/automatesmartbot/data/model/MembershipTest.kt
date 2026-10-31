package com.example.automatesmartbot.data.model

import org.junit.Assert.*
import org.junit.Test

class MembershipTest {
    private fun member(flag: Boolean?, name: String?) = UserMeResponse(null, null, null, null, flag, name)
    @Test fun authoritativeFalseDoesNotUnlockStaleProLabel() {
        assertFalse(member(false, "pro").hasProMembership)
        assertTrue(member(true, "free").hasProMembership)
        assertTrue(member(null, "pro").hasProMembership)
        assertFalse(member(null, null).hasProMembership)
    }
}
