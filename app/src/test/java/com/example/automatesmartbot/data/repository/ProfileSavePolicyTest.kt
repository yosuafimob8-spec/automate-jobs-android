package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.ProfileUpdateResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSavePolicyTest {

    @Test
    fun missingResponseIsNotConfirmed() {
        assertFalse(ProfileSavePolicy.isConfirmed(null))
    }

    @Test
    fun falseResponseIsNotConfirmed() {
        assertFalse(ProfileSavePolicy.isConfirmed(ProfileUpdateResponse(success = false, completed = false)))
        assertFalse(ProfileSavePolicy.isConfirmed(ProfileUpdateResponse(success = null, completed = null)))
    }

    @Test
    fun serverSuccessIsConfirmed() {
        assertTrue(ProfileSavePolicy.isConfirmed(ProfileUpdateResponse(success = true, completed = false)))
    }

    @Test
    fun serverCompletedFlagIsConfirmed() {
        assertTrue(ProfileSavePolicy.isConfirmed(ProfileUpdateResponse(success = false, completed = true)))
    }
}
