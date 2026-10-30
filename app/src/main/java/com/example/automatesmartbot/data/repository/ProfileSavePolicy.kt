package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.ProfileUpdateResponse

object ProfileSavePolicy {
    fun isConfirmed(response: ProfileUpdateResponse?): Boolean {
        return response?.success == true || response?.completed == true
    }
}
