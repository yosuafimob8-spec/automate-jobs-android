package com.example.automatesmartbot.data.autoapply

/** Missing/null references must never be interpreted as consent or Gmail delivery. */
object AutoApplyConfirmation {
    fun hasReference(value: String?): Boolean = !value.isNullOrBlank() && !value.trim().equals("null", ignoreCase = true)
    fun consent(explicit: Boolean, timestamp: String?, recordedForAccount: Boolean): Boolean =
        explicit || hasReference(timestamp) || recordedForAccount
    fun consentState(serverConsent: Boolean?, timestamp: String?, cached: Boolean): Boolean =
        serverConsent ?: consent(false, timestamp, cached)
    fun sent(status: String, gmailId: String?): Boolean = status == "sent" && hasReference(gmailId)
}
