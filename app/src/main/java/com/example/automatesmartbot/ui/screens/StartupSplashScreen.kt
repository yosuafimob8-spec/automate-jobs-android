package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.components.BrandLogo
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

@Composable
fun StartupSplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color(0xFF0963B3), Color(0xFF0B4B8B), Color(0xFF063D78))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val wave = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, size.height * .76f)
                cubicTo(size.width * .3f, size.height * .62f, size.width * .65f, size.height * .88f, size.width, size.height * .65f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(wave, Color(0xFF55C6A9).copy(alpha = .45f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandLogo(modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(14.dp))
            com.example.automatesmartbot.ui.components.BrandWordmark(fontSize = 34.sp, onDark = true)
            Text("Your Career Journey", color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.height(30.dp))
            CircularProgressIndicator(color = Color(0xFF55C6A9), strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
        }
    }
}
