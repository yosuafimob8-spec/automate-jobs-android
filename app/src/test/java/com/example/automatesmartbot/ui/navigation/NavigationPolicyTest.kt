package com.example.automatesmartbot.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationPolicyTest {

    @Test
    fun validSessionStartsAtDashboardEvenWhenOffline() {
        assertEquals(Screen.Dashboard.route, NavigationPolicy.startRoute(hasValidSession = true))
    }

    @Test
    fun missingOrExpiredSessionStartsAtLogin() {
        assertEquals(Screen.Login.route, NavigationPolicy.startRoute(hasValidSession = false))
    }

    @Test
    fun incompleteProfileGoesToOnboardingAfterLogin() {
        assertEquals(
            Screen.OnboardingProfile.route,
            NavigationPolicy.postLoginRoute(profileCompleted = false)
        )
    }

    @Test
    fun completeOrUnknownProfileGoesToDashboardAfterLogin() {
        assertEquals(
            Screen.Dashboard.route,
            NavigationPolicy.postLoginRoute(profileCompleted = true)
        )
        assertEquals(
            Screen.Dashboard.route,
            NavigationPolicy.postLoginRoute(profileCompleted = null)
        )
    }

    @Test
    fun offlineProfileCheckKeepsUserOnDashboard() {
        assertNull(
            NavigationPolicy.profileGateTarget(
                profileCompleted = null,
                currentRoute = Screen.Dashboard.route
            )
        )
    }

    @Test
    fun incompleteProfileRedirectsOnlyFromDashboard() {
        assertEquals(
            Screen.OnboardingProfile.route,
            NavigationPolicy.profileGateTarget(
                profileCompleted = false,
                currentRoute = Screen.Dashboard.route
            )
        )
        assertNull(
            NavigationPolicy.profileGateTarget(
                profileCompleted = false,
                currentRoute = Screen.Profil.route
            )
        )
    }

    @Test
    fun completedProfileLeavesOnboardingWithoutLooping() {
        assertEquals(
            Screen.Dashboard.route,
            NavigationPolicy.profileGateTarget(
                profileCompleted = true,
                currentRoute = Screen.OnboardingProfile.route
            )
        )
        assertNull(
            NavigationPolicy.profileGateTarget(
                profileCompleted = true,
                currentRoute = Screen.Dashboard.route
            )
        )
    }
}
