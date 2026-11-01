package com.example.automatesmartbot.ui.navigation

object NavigationPolicy {
    fun startRoute(hasValidSession: Boolean): String {
        return if (hasValidSession) Screen.Dashboard.route else Screen.Login.route
    }

    fun postLoginRoute(profileCompleted: Boolean?): String {
        return if (profileCompleted != true) {
            Screen.OnboardingProfile.route
        } else {
            Screen.Dashboard.route
        }
    }

    fun profileGateTarget(profileCompleted: Boolean?, currentRoute: String?): String? {
        return when {
            profileCompleted == false && currentRoute == Screen.Dashboard.route -> {
                Screen.OnboardingProfile.route
            }
            profileCompleted == true && currentRoute == Screen.OnboardingProfile.route -> {
                Screen.Dashboard.route
            }
            else -> null
        }
    }
}
