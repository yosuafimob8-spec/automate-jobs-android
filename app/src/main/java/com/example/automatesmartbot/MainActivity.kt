package com.example.automatesmartbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.ui.screens.MainDashboardScreen
import com.example.automatesmartbot.ui.theme.KarirMateTheme
import com.example.automatesmartbot.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current.applicationContext
            val themePreference = remember { ThemePreference(context) }
            KarirMateTheme(mode = themePreference.mode) {
                MainDashboardScreen(themePreference = themePreference)
            }
        }
    }
}
