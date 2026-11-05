package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.automatesmartbot.R
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
import androidx.compose.ui.text.font.FontStyle
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
            val w = size.width
            val h = size.height
            fun wave(color: Color, path: androidx.compose.ui.graphics.Path) { drawPath(path, color) }
            wave(Color(0xFF168CC4), androidx.compose.ui.graphics.Path().apply {
                moveTo(0f,h*.79f); cubicTo(w*.25f,h*.83f,w*.65f,h*.61f,w,h*.61f)
                lineTo(w,h); lineTo(0f,h); close()
            })
            wave(Color(0xFF46CEB5), androidx.compose.ui.graphics.Path().apply {
                moveTo(w*.60f,h*.74f); cubicTo(w*.75f,h*.67f,w*.91f,h*.62f,w,h*.60f)
                lineTo(w,h*.83f); cubicTo(w*.87f,h*.78f,w*.72f,h*.75f,w*.60f,h*.74f); close()
            })
            wave(Color(0xFF0860A7), androidx.compose.ui.graphics.Path().apply {
                moveTo(0f,h*.87f); cubicTo(w*.31f,h*.77f,w*.48f,h*.72f,w*.74f,h*.77f)
                cubicTo(w*.86f,h*.79f,w*.96f,h*.83f,w,h*.85f)
                lineTo(w,h); lineTo(0f,h); close()
            })
            wave(Color(0xFF03477E), androidx.compose.ui.graphics.Path().apply {
                moveTo(0f,h*.70f); cubicTo(w*.11f,h*.73f,w*.20f,h*.76f,w*.30f,h*.77f)
                lineTo(0f,h*.84f); close()
            })
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
            Image(
                painter = painterResource(R.drawable.karirmate_brand_mark),
                contentDescription = "Logo KarirMate",
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "KarirMate", color = Color.White, fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic
            )
            Text("Your Career Journey", color = Color.White, fontSize = 17.sp)
        }
    }
}
