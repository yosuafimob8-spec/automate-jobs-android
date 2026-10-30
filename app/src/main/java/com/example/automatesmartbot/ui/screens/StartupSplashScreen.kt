package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.DarkBackground
import com.example.automatesmartbot.ui.theme.LightBlueAccent
import com.example.automatesmartbot.ui.theme.TextGray
import com.example.automatesmartbot.ui.theme.TextWhite

@Composable
fun StartupSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = Color(0xFF1B263B),
                shape = CircleShape,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = LightBlueAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            Text(
                text = "AutoMateSmartBot",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextWhite
            )
            Text(
                text = "Menyiapkan akun Anda",
                fontSize = 12.sp,
                color = TextGray
            )
            CircularProgressIndicator(
                color = LightBlueAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
