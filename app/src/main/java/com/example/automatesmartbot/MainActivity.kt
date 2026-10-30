package com.example.automatesmartbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.ui.screens.MainDashboardScreen
import com.example.automatesmartbot.ui.theme.AutoMateSmartBotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        enableEdgeToEdge()
        setContent {
            AutoMateSmartBotTheme {
                MainDashboardScreen()
            }
        }
    }
}
