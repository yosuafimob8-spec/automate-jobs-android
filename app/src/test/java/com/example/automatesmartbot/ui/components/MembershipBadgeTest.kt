package com.example.automatesmartbot.ui.components

import com.example.automatesmartbot.data.model.DashboardStatusResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class MembershipBadgeTest {
    private fun status(pro: Boolean, verified: Boolean) = DashboardStatusResponse(
        status = "Online", isPro = pro, membershipVerified = verified, botStatus = "Online", isOnline = true
    )

    @Test fun verifiedPaidAccountDisplaysPro() {
        assertEquals(MembershipTier.PRO, membershipTier(status(pro = true, verified = true)))
    }

    @Test fun verifiedFreeAccountDisplaysFree() {
        assertEquals(MembershipTier.FREE, membershipTier(status(pro = false, verified = true)))
    }

    @Test fun unavailableOrUnverifiedMembershipIsNotMisrepresentedAsFreeOrPro() {
        assertEquals(MembershipTier.UNVERIFIED, membershipTier(null))
        assertEquals(MembershipTier.UNVERIFIED, membershipTier(status(pro = false, verified = false)))
        assertEquals(MembershipTier.UNVERIFIED, membershipTier(status(pro = true, verified = false)))
    }
}
