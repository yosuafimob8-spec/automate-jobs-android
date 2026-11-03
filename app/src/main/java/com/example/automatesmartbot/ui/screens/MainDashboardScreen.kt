package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.repository.ProfileSetupRepository
import com.example.automatesmartbot.data.security.SessionManager
import com.example.automatesmartbot.ui.components.AppTopBar
import com.example.automatesmartbot.ui.navigation.NavigationPolicy
import com.example.automatesmartbot.ui.navigation.Screen
import com.example.automatesmartbot.ui.navigation.appShellItems
import com.example.automatesmartbot.ui.navigation.bottomNavItems
import com.example.automatesmartbot.ui.screens.auth.ForgotPasswordScreen
import com.example.automatesmartbot.ui.screens.auth.LoginScreen
import com.example.automatesmartbot.ui.screens.auth.RegisterScreen
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import com.example.automatesmartbot.ui.viewmodel.DashboardViewModel
import com.example.automatesmartbot.ui.theme.ThemePreference
import com.example.automatesmartbot.ui.theme.FixedCanvasTheme

@Composable
fun MainDashboardScreen(themePreference: ThemePreference) {
    val sessionManager = remember { ApiClient.getSessionManager() }
    var startupComplete by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf<String?>(null) }
    var initialSessionToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val validToken = sessionManager?.getValidToken()
        initialSessionToken = validToken
        startDestination = NavigationPolicy.startRoute(validToken != null)
        startupComplete = true
    }

    if (!startupComplete || startDestination == null) {
        StartupSplashScreen()
        return
    }

    MainDashboardContent(
        startDestination = startDestination!!,
        initialSessionToken = initialSessionToken,
        sessionManager = sessionManager,
        themePreference = themePreference
    )
}

@Composable
private fun MainDashboardContent(
    startDestination: String,
    initialSessionToken: String?,
    sessionManager: SessionManager?,
    themePreference: ThemePreference
) {
    val navController = rememberNavController()
    var sessionToken by remember { mutableStateOf(initialSessionToken) }
    var checkedToken by remember { mutableStateOf<String?>(null) }
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dashboardStatus by dashboardViewModel.status.collectAsState()
    val cvViewModel: CvBuilderViewModel = viewModel(key = "cv_builder_${sessionToken?.hashCode() ?: 0}")
    val cvHistory by cvViewModel.history.collectAsState()

    val navigateToDashboard: () -> Unit = {
        if (!navController.popBackStack(Screen.Dashboard.route, inclusive = false)) {
            navController.navigate(Screen.Dashboard.route) {
                launchSingleTop = true
            }
        }
    }

    val navigateToLogin: () -> Unit = {
        if (!navController.popBackStack(Screen.Login.route, inclusive = false)) {
            navController.navigate(Screen.Login.route) {
                launchSingleTop = true
            }
        }
    }

    val navigateUpOrDashboard: () -> Unit = {
        if (!navController.popBackStack()) {
            navigateToDashboard()
        }
    }

    val endSession: () -> Unit = {
        sessionToken = null
        checkedToken = null
        dashboardViewModel.reset()
        if (navController.currentDestination?.route != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Dashboard.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(sessionManager) {
        sessionManager?.sessionExpired?.collect {
            endSession()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sessionToken) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                sessionToken != null &&
                sessionManager?.hasValidSession() != true
            ) {
                endSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(sessionToken) {
        val token = sessionToken
        if (token.isNullOrBlank()) {
            dashboardViewModel.reset()
            return@LaunchedEffect
        }

        dashboardViewModel.loadStatus()

        if (checkedToken == token) return@LaunchedEffect
        checkedToken = token

        val completed = ProfileSetupRepository(sessionManager).checkServerProfileCompletion()
        val currentRoute = navController.currentDestination?.route
        when (NavigationPolicy.profileGateTarget(completed, currentRoute)) {
            Screen.OnboardingProfile.route -> {
                navController.navigate(Screen.OnboardingProfile.route) {
                    launchSingleTop = true
                }
            }
            Screen.Dashboard.route -> navigateToDashboard()
        }
    }

    val isPro = dashboardStatus?.isPro == true
    val isOnline = dashboardStatus?.isOnline == true

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    val currentTitle = when (currentRoute) {
        Screen.Dashboard.route -> "Beranda"
        Screen.CariLowongan.route -> "Cari Lowongan"
        Screen.KirimLamaran.route -> "Kirim Lamaran"
        Screen.JobTraining.route -> "Pelatihan Kerja"
        Screen.SalaryCalculator.route -> "Kalkulator Gaji"
        Screen.CareerLetters.route -> "Surat Lamaran & Resign"
        Screen.CareerDocuments.route -> "Dokumen Karier"
        Screen.CvBuilder.route -> cvHistory.last()
        Screen.LatihanPsikotes.route -> "Latihan Psikotes"
        Screen.AiInterview.route -> "AI Interview"
        Screen.WorkPedia.route -> "WorkPedia"
        Screen.Riwayat.route -> "Riwayat Lamaran"
        Screen.SuratIzinDokumen.route -> "Surat Izin & Dokumen Kerja"
        Screen.Profil.route -> "Profil & Pengaturan"
        Screen.Login.route -> "Masuk Akun"
        Screen.Register.route -> "Daftar Akun"
        Screen.ForgotPassword.route -> "Reset Password"
        Screen.OnboardingProfile.route -> "Setup Profil"
        else -> "Karir Mate"
    }

    val bottomNavRoutes = bottomNavItems.map { it.route }
    val appShellRoutes = appShellItems.map { it.route }
    val showBottomBar = currentRoute in appShellRoutes
    val showTopBar = currentRoute in appShellRoutes
    val showBack = currentRoute in appShellRoutes && currentRoute != Screen.Dashboard.route
    val cvBack: () -> Unit = { if (!cvViewModel.goBack()) navigateUpOrDashboard() }
    val onBack: (() -> Unit)? = if (!showBack) null else if (currentRoute == Screen.CvBuilder.route) cvBack else navigateUpOrDashboard

    BackHandler(enabled = currentRoute == Screen.OnboardingProfile.route) {
        navigateUpOrDashboard()
    }

    FixedCanvasTheme(dark = false) {
    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    title = currentTitle,
                    isPro = isPro,
                    onNavigateProfil = {
                        navController.navigate(Screen.Profil.route) {
                            launchSingleTop = true
                        }
                    },
                    onBack = onBack
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontSize = 11.sp, maxLines = 1) },
                            selected = selected,
                            onClick = {
                                if (screen.route == Screen.Dashboard.route) {
                                    cvViewModel.resetNavigation()
                                    navigateToDashboard()
                                } else if (!selected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                FixedCanvasTheme(dark = false) {
                    DashboardScreen(
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        viewModel = dashboardViewModel
                    )

                }
            }
            composable(Screen.CariLowongan.route) {
                FixedCanvasTheme(dark = false) {
                    CariLowonganScreen(isPro = isPro)

                }
            }
            composable(Screen.KirimLamaran.route) {
                KirimLamaranScreen(
                    isPro = isPro,
                    isOnline = isOnline,
                    onNavigateProfil = {
                        navController.navigate(Screen.Profil.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.JobTraining.route) { JobTrainingScreen() }
            composable(Screen.SalaryCalculator.route) { SalaryCalculatorScreen() }
            composable(Screen.CareerLetters.route) {
                CareerLettersScreen(onApplicationLetter = {
                    cvViewModel.resetNavigation()
                    cvViewModel.navigate("Pilih Jenis Surat")
                    navController.navigate(Screen.CvBuilder.route) { launchSingleTop = true }
                })
            }
            composable(Screen.CareerDocuments.route) {
                CareerDocumentsScreen(onLegacyDocuments = { navController.navigate(Screen.SuratIzinDokumen.route) { launchSingleTop = true } })
            }
            composable(Screen.CvBuilder.route) {
                CvBuilderScreen(viewModel = cvViewModel, onBack = cvBack)
            }
            composable(Screen.LatihanPsikotes.route) {
                LatihanPsikotesScreen(isPro = isPro, onExit = navigateUpOrDashboard)
            }
            composable(Screen.AiInterview.route) {
                FixedCanvasTheme(dark = false) {
                    AiInterviewScreen(isPro = isPro)

                }
            }
            composable(Screen.WorkPedia.route) { WorkPediaScreen(onExit = navigateUpOrDashboard) }
            composable(Screen.SuratIzinDokumen.route) {
                FixedCanvasTheme(dark = false) {
                    SuratIzinDokumenScreen()

                }
            }
            composable(Screen.Riwayat.route) {
                FixedCanvasTheme(dark = false) {
                    RiwayatScreen()

                }
            }
            composable(Screen.Profil.route) {
                ProfilScreen(
                    isOnline = isOnline,
                    onNavigateLogin = endSession,
                    themePreference = themePreference,
                    onNavigateEditProfile = {
                        navController.navigate(Screen.OnboardingProfile.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                FixedCanvasTheme(dark = true) {
                    LoginScreen(
                        onNavigateRegister = {
                            navController.navigate(Screen.Register.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateForgot = {
                            navController.navigate(Screen.ForgotPassword.route) {
                                launchSingleTop = true
                            }
                        },
                        onLoginSuccess = { profileCompleted ->
                            val newToken = sessionManager?.getValidToken()
                            sessionToken = newToken
                            checkedToken = newToken
                            val target = NavigationPolicy.postLoginRoute(profileCompleted)
                            navController.navigate(target) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )

                }
            }
            composable(Screen.Register.route) {
                FixedCanvasTheme(dark = true) {
                    RegisterScreen(
                        onNavigateLogin = navigateToLogin,
                        onRegisterSuccess = {
                            navigateToLogin()
                        }
                    )

                }
            }
            composable(Screen.ForgotPassword.route) {
                FixedCanvasTheme(dark = true) {
                    ForgotPasswordScreen(
                        onNavigateLogin = navigateToLogin
                    )

                }
            }
            composable(Screen.OnboardingProfile.route) {
                FixedCanvasTheme(dark = true) {
                    OnboardingProfileScreen(
                        isOnline = isOnline,
                        onBack = navigateUpOrDashboard,
                        onProfileSaved = {
                            checkedToken = sessionToken
                            dashboardViewModel.loadStatus()
                            navigateToDashboard()
                        }
                    )

                }
            }
        }
    }
    }
}
