package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

/** Scalable offline illustration, no external image or licensing dependency. */
@Composable
fun TrainingHeroIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(164.dp)
            .background(Color(0xFFFFE8CF), RoundedCornerShape(20.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color(0xFFFFD29C), radius = h * .44f, center = Offset(w * .72f, h * .50f))
            drawCircle(Color(0xFFFFC17C), radius = h * .28f, center = Offset(w * .89f, h * .20f))
            drawRoundRect(Color.White, topLeft = Offset(w * .48f, h * .17f), size = Size(w * .30f, h * .67f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f))
            drawRoundRect(Color(0xFFFF6A0F), topLeft = Offset(w * .53f, h * .26f), size = Size(w * .18f, h * .055f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            repeat(3) { index ->
                drawRoundRect(Color(0xFFFFDEC9), topLeft = Offset(w * .53f, h * (.40f + index * .12f)), size = Size(w * .17f, h * .045f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f))
            }
            drawCircle(Color(0xFF0F172A), radius = h * .11f, center = Offset(w * .84f, h * .42f))
            drawRoundRect(Color(0xFFFF6B00), topLeft = Offset(w * .74f, h * .55f), size = Size(w * .22f, h * .32f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f))
            drawCircle(Color(0xFFFFBD75), radius = h * .07f, center = Offset(w * .84f, h * .42f))
        }
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.47f).padding(14.dp)) {
            Text("BELAJAR", color = KarirOrange, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("Skill baru, karier maju!", color = KarirTextDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun TrainingLessonVisual(title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("▣", color = KarirOrange, fontSize = 48.sp)
            Text(title, color = KarirTextDark, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Materi interaktif • Belajar mandiri", color = KarirTextMuted, fontSize = 12.sp)
        }
    }
}
