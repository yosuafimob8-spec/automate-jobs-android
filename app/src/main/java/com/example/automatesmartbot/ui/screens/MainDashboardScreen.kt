package com.example.automatesmartbot.ui.screens

import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    val cvViewModel: CvBuilderViewModel = viewModel(key = "cv_builder_${com.example.automatesmartbot.data.cv.cvSessionKey() ?: "signed-out"}")
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

    val isPro = dashboardStatus?.isPro == true && dashboardStatus?.membershipVerified == true
    val isOnline = dashboardStatus?.isOnline == true

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    val currentTitle = when (currentRoute) {
        Screen.Dashboard.route -> "Beranda"
        Screen.CariLowongan.route -> "Cari Lowongan"
        Screen.KirimLamaran.route -> "Kirim Lamaran"
        Screen.JobTraining.route -> "Pelatihan Kerja"
        Screen.Certificates.route -> "Sertifikat"
        Screen.SalaryCalculator.route -> "Kalkulator Gaji"
        Screen.CareerLetters.route -> "Surat Lamaran & Resign"
        Screen.CareerDocuments.route -> "Dokumen Karier"
        Screen.CvBuilder.route -> cvHistory.last()
        Screen.LatihanPsikotes.route -> "Latihan Psikotes"
        Screen.AiInterview.route -> "AI Interview"
        Screen.WorkPedia.route -> "WorkPedia"
        Screen.Riwayat.route -> "Riwayat Lamaran"
        Screen.SuratIzinDokumen.route -> "Dokumen Karier"
        Screen.UpgradePro.route -> "Upgrade PRO"
        Screen.ProCheckout.route -> "Pembelian Paket"
        Screen.ManagePro.route -> "Kelola Langganan"
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
    val showTopBar = currentRoute in appShellRoutes && currentRoute != Screen.Dashboard.route && currentRoute != Screen.AiInterview.route
    val showBack = currentRoute in appShellRoutes && currentRoute != Screen.Dashboard.route
    val cvBack: () -> Unit = { if (!cvViewModel.goBack()) navigateUpOrDashboard() }
    var trainingBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    val edgeHeaderRoutes = setOf(
        Screen.LatihanPsikotes.route, Screen.SalaryCalculator.route,
        Screen.JobTraining.route, Screen.Certificates.route
    )
    val edgeHeader = currentRoute in edgeHeaderRoutes
    var psychotestBack by remember { mutableStateOf<(() -> Unit)?>(null) }
    val onBack: (() -> Unit)? = when {
        !showBack -> null
        currentRoute == Screen.CvBuilder.route -> cvBack
        currentRoute == Screen.LatihanPsikotes.route -> { { psychotestBack?.invoke() ?: navigateUpOrDashboard() } }
        currentRoute == Screen.JobTraining.route || currentRoute == Screen.Certificates.route -> { { trainingBack?.invoke() ?: navigateUpOrDashboard() } }
        else -> navigateUpOrDashboard
    }

    BackHandler(enabled = currentRoute == Screen.OnboardingProfile.route) {
        navigateUpOrDashboard()
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                if (edgeHeader) {
                    CareerEdgeHeader(
                        title = currentTitle,
                        subtitle = when (currentRoute) {
                            Screen.LatihanPsikotes.route -> "Latihan Tes Kerja & Persiapan CPNS"
                            Screen.SalaryCalculator.route -> "Simulasi gaji, lembur dan potongan"
                            Screen.JobTraining.route -> "Upgrade skill untuk dunia kerja"
                            else -> "Riwayat dan verifikasi sertifikat"
                        },
                        isPro = isPro,
                        onBack = onBack ?: navigateUpOrDashboard,
                        onProfile = { navController.navigate(Screen.Profil.route) { launchSingleTop = true } }
                    )
                } else {
                    AppTopBar(
                        title = currentTitle,
                        isPro = isPro,
                        onNavigateProfil = { navController.navigate(Screen.Profil.route) { launchSingleTop = true } },
                        onBack = onBack
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = com.example.automatesmartbot.ui.theme.screenCanvas(Color(0xFFF5FAFC), com.example.automatesmartbot.ui.theme.DarkSurface),
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(width = 0.75.dp, color = Color(0xFFC6DFE8))
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = {
                                Text(
                                    screen.title,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            },
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
                                selectedIconColor = com.example.automatesmartbot.ui.theme.OnKarirOrange,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = Color(0xFF2563A6)
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
                    DashboardScreen(
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        viewModel = dashboardViewModel
                    )

            }
            composable(Screen.UpgradePro.route) { UpgradeProScreen(onCheckout = { navController.navigate("pro_checkout/$it") }) }
            composable(Screen.ProCheckout.route, arguments = listOf(navArgument("plan") { type = NavType.IntType })) { entry ->
                ProCheckoutScreen(entry.arguments?.getInt("plan") ?: 0, onComplete = { navController.navigate(Screen.ManagePro.route) })
            }
            composable(Screen.ManagePro.route) { ManageProSubscriptionScreen(onUpgrade = { navController.navigate(Screen.UpgradePro.route) }) }
            composable(Screen.CariLowongan.route) {
                FixedCanvasTheme(dark = when (themePreference.mode) {
                    com.example.automatesmartbot.ui.theme.ThemeMode.DARK -> true
                    com.example.automatesmartbot.ui.theme.ThemeMode.LIGHT -> false
                    com.example.automatesmartbot.ui.theme.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
                }) {
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
            composable(Screen.JobTraining.route) { JobTrainingScreen(onExit = navigateUpOrDashboard, onBackActionChanged = { trainingBack = it }) }
            composable(Screen.Certificates.route) { JobTrainingScreen(initialStage = "history", onExit = navigateUpOrDashboard, onBackActionChanged = { trainingBack = it }) }
            composable(Screen.SalaryCalculator.route) { SalaryCalculatorScreen() }
            composable(Screen.CareerLetters.route) {
                CareerLettersScreen(onApplicationLetter = {
                    cvViewModel.resetNavigation()
                    cvViewModel.navigate("Pilih Jenis Surat")
                    navController.navigate(Screen.CvBuilder.route) { launchSingleTop = true }
                })
            }
            composable(Screen.CareerDocuments.route) {
                CareerDocumentsScreen()
            }
            composable(Screen.CvBuilder.route) {
                CvBuilderScreen(viewModel = cvViewModel, onBack = cvBack, isPro = isPro)
            }
            composable(Screen.LatihanPsikotes.route) {
                LatihanPsikotesScreen(isPro = isPro, onExit = navigateUpOrDashboard, onBackActionChanged = { action -> psychotestBack = action })
            }
            composable(Screen.AiInterview.route) {
                FixedCanvasTheme(dark = when (themePreference.mode) {
                    com.example.automatesmartbot.ui.theme.ThemeMode.DARK -> true
                    com.example.automatesmartbot.ui.theme.ThemeMode.LIGHT -> false
                    com.example.automatesmartbot.ui.theme.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
                }) {
                    AiInterviewScreen(isPro = isPro, onExit = navigateUpOrDashboard)

                }
            }
            composable(Screen.WorkPedia.route) { WorkPediaScreen(onExit = navigateUpOrDashboard) }
            composable(Screen.SuratIzinDokumen.route) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(Screen.CareerDocuments.route) {
                        popUpTo(Screen.SuratIzinDokumen.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            composable(Screen.Riwayat.route) {
                FixedCanvasTheme(dark = when (themePreference.mode) {
                    com.example.automatesmartbot.ui.theme.ThemeMode.DARK -> true
                    com.example.automatesmartbot.ui.theme.ThemeMode.LIGHT -> false
                    com.example.automatesmartbot.ui.theme.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
                }) {
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
            composable(Screen.Register.route) {
                FixedCanvasTheme(dark = when (themePreference.mode) {
                    com.example.automatesmartbot.ui.theme.ThemeMode.DARK -> true
                    com.example.automatesmartbot.ui.theme.ThemeMode.LIGHT -> false
                    com.example.automatesmartbot.ui.theme.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
                }) {
                    RegisterScreen(
                        onNavigateLogin = navigateToLogin,
                        onRegisterSuccess = {
                            navigateToLogin()
                        }
                    )

                }
            }
            composable(Screen.ForgotPassword.route) {
                FixedCanvasTheme(dark = when (themePreference.mode) {
                    com.example.automatesmartbot.ui.theme.ThemeMode.DARK -> true
                    com.example.automatesmartbot.ui.theme.ThemeMode.LIGHT -> false
                    com.example.automatesmartbot.ui.theme.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
                }) {
                    ForgotPasswordScreen(
                        onNavigateLogin = navigateToLogin
                    )

                }
            }
            composable(Screen.OnboardingProfile.route) {
                FixedCanvasTheme(dark = when (themePreference.mode) {
                    com.example.automatesmartbot.ui.theme.ThemeMode.DARK -> true
                    com.example.automatesmartbot.ui.theme.ThemeMode.LIGHT -> false
                    com.example.automatesmartbot.ui.theme.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
                }) {
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
