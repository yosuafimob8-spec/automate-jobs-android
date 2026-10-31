package com.example.automatesmartbot.data.autoapply

import org.junit.Assert.*
import org.junit.Test

class AutoApplyConfirmationTest {
    @Test fun jsonNullNeverSkipsExplicitConsent() {
        assertFalse(AutoApplyConfirmation.consent(false, "null", false))
        assertFalse(AutoApplyConfirmation.consent(false, null, false))
        assertTrue(AutoApplyConfirmation.consent(false, "2026-10-31T10:00:00Z", false))
        assertTrue(AutoApplyConfirmation.consent(false, null, true))
    }
    @Test fun revokedServerConsentOverridesLocalCacheAndOldTimestamp() {
        assertFalse(AutoApplyConfirmation.consentState(false,"2026-10-31T10:00:00Z",true))
        assertTrue(AutoApplyConfirmation.consentState(true,null,false))
    }
    @Test fun statusAloneIsNotGmailConfirmation() {
        assertFalse(AutoApplyConfirmation.sent("sent", "null"))
        assertFalse(AutoApplyConfirmation.sent("sent", ""))
        assertFalse(AutoApplyConfirmation.sent("unknown", "message-id"))
        assertTrue(AutoApplyConfirmation.sent("sent", "message-id"))
    }
}
