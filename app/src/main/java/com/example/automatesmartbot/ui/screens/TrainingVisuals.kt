package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Scalable offline illustration, no external image or licensing dependency. */
@Composable
fun TrainingHeroIllustration(modifier: Modifier = Modifier) {
    Box(modifier.height(164.dp).background(Color(0xFFFFE8CF), RoundedCornerShape(20.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawCircle(Color(0xFFFFD29C), radius = h * .44f, center = Offset(w * .72f, h * .50f))
            drawCircle(Color(0xFFFFC17C), radius = h * .28f, center = Offset(w * .89f, h * .20f))
            drawRoundRect(Color.White, topLeft = Offset(w * .48f, h * .17f), size = Size(w * .30f, h * .67f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f))
            drawRoundRect(Color(0xFFFF6A0F), topLeft = Offset(w * .53f, h * .26f), size = Size(w * .18f, h * .055f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            repeat(3) { index ->
                drawRoundRect(Color(0xFF8ECBC0), topLeft = Offset(w * .53f, h * (.40f + index * .12f)), size = Size(w * .17f, h * .045f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
            }
            drawCircle(Color(0xFF18354D), radius = h * .11f, center = Offset(w * .84f, h * .42f))
            drawRoundRect(Color(0xFF009E88), topLeft = Offset(w * .74f, h * .55f), size = Size(w * .22f, h * .32f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f))
            drawCircle(Color(0xFFFFBD75), radius = h * .07f, center = Offset(w * .84f, h * .42f))
        }
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.47f).padding(14.dp)) {
            Text("BELAJAR", color = Color(0xFFFF690D), fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("Skill baru, karier maju!", color = Color(0xFF152D43), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun TrainingLessonVisual(title: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F7F3))) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("▣", color = Color(0xFF009F89), fontSize = 48.sp)
            Text(title, color = Color(0xFF152D43), fontWeight = FontWeight.Bold)
            Text("Materi interaktif • Belajar mandiri", color = Color(0xFF009F89), fontSize = 12.sp)
        }
    }
}
