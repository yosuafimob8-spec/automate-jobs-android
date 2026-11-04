package com.example.automatesmartbot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Beranda", Icons.Default.Dashboard)
    object CariLowongan : Screen("cari_lowongan", "Lowongan", Icons.Default.Work)
    object KirimLamaran : Screen("kirim_lamaran", "Lamaran", Icons.AutoMirrored.Filled.Send)
    object CvBuilder : Screen("cv_builder", "CV Builder", Icons.Default.Description)
    object LatihanPsikotes : Screen("latihan_psikotes", "Psikotes", Icons.Default.Psychology)
    object JobTraining : Screen("job_training", "Pelatihan Kerja", Icons.Default.Work)
    object SalaryCalculator : Screen("salary_calculator", "Kalkulator Gaji", Icons.Default.Work)
    object CareerLetters : Screen("career_letters", "Surat Lamaran & Resign", Icons.Default.Description)
    object CareerDocuments : Screen("career_documents", "Dokumen Karier", Icons.Default.FolderShared)
    object AiInterview : Screen("ai_interview", "AI Interview", Icons.Default.Mic)
    object AiAgent : Screen("ai_agent", "AI Agent", Icons.Default.SmartToy)
    object Riwayat : Screen("riwayat", "Riwayat", Icons.Default.History)
    object Profil : Screen("profil", "Profil", Icons.Default.Person)
    object Login : Screen("login", "Masuk", Icons.Default.Lock)
    object Register : Screen("register", "Daftar", Icons.Default.Person)
    object ForgotPassword : Screen("forgot_password", "Lupa Password", Icons.Default.VpnKey)
    object OnboardingProfile : Screen("onboarding_profile", "Lengkapi Profil", Icons.Default.Person)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.CariLowongan,
    Screen.KirimLamaran,
    Screen.Riwayat,
    Screen.Profil
)

val detailNavItems = listOf(
    Screen.JobTraining, Screen.SalaryCalculator, Screen.CareerLetters, Screen.CareerDocuments,
    Screen.CvBuilder,
    Screen.LatihanPsikotes,
    Screen.AiInterview,
    Screen.AiAgent,
)

val appShellItems = bottomNavItems + detailNavItems
