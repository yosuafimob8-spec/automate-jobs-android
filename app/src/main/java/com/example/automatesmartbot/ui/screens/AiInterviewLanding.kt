package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.R

private val interviewBlue = Color(0xFF0968B8)
private val interviewNavy = Color(0xFF102B4B)

/** Single back control for all AI Interview stages; the parent app bar is hidden on this route. */
@Composable
internal fun InterviewBrandedHeader(onBack: () -> Unit, isPro: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().height(124.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF075DAA), Color(0xFF006DAB), Color(0xFF049CA7))))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val wave = Path().apply {
                moveTo(size.width * .62f, 0f)
                cubicTo(size.width * .69f, size.height * .36f, size.width * .90f, size.height * .22f, size.width, size.height * .78f)
                lineTo(size.width, 0f); close()
            }
            drawPath(wave, Color(0xFF29B8CB).copy(alpha = .23f))
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = .07f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .40f))
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali satu tahap", tint = Color.White)
                }
            }
            Image(
                painter = painterResource(R.drawable.karirmate_brand_mark),
                contentDescription = "Logo KarirMate",
                modifier = Modifier.size(52.dp),
                contentScale = ContentScale.Fit
            )
            Column(Modifier.weight(1f)) {
                Text("AI Interview", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Latihan Wawancara Kerja dengan AI", fontSize = 11.sp, color = Color.White.copy(alpha = .90f), maxLines = 2)
            }
            com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.HEADER)
        }
    }
}

@Composable
internal fun InterviewModeLanding(onTextSelected: () -> Unit, onVoiceSelected: () -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val edge = if (dark) Color(0xFF315371) else Color(0xFFD1E2EE)
    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(surface, if (dark) Color(0xFF10253A) else Color(0xFFEAF5FD)))
        ).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.karirmate_brand_mark),
                contentDescription = null,
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("KarirMate", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (dark) Color(0xFF352A1D) else Color(0xFFFFF4E3),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF8C778))
            ) {
                Text("✦  AI INTERVIEW", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = if (dark) Color(0xFFFFCB7A) else Color(0xFFC66B05),
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(194.dp).clip(RoundedCornerShape(25.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFE7F6FF), Color(0xFFD1E9FC))))
                .border(1.dp, Color.White, RoundedCornerShape(25.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.ai_interview_hr_portrait),
                contentDescription = "Ilustrasi HR AI virtual",
                modifier = Modifier.align(Alignment.BottomEnd).fillMaxHeight().width(166.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(.67f).padding(start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("AI Interview", color = interviewNavy, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Latihan wawancara kerja interaktif dengan AI HR yang cerdas, adaptif, dan siap memberi umpan balik.",
                    color = interviewNavy, fontSize = 13.sp, lineHeight = 20.sp
                )
            }
        }

        InterviewModeCard(
            title = "Interview Teks",
            body = "Wawancara interaktif via chat tanya-jawab AI",
            selected = true, isDark = dark, onClick = onTextSelected,
            icon = { Icon(Icons.Default.ChatBubbleOutline, null, tint = interviewBlue, modifier = Modifier.size(34.dp)) }
        )
        InterviewModeCard(
            title = "Interview Suara",
            body = "Wawancara dengan suara dan pengenalan audio",
            selected = false, isDark = dark, onClick = onVoiceSelected,
            icon = { Icon(Icons.Default.Mic, null, tint = Color(0xFF6553D7), modifier = Modifier.size(36.dp)) }
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun InterviewModeCard(
    title: String,
    body: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) Color(0xFF1268AD) else if (isDark) Color(0xFF39536C) else Color(0xFFD2E0EC)),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(74.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF223E58) else Color(0xFFE0F1FF)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(14.dp))
            Box(Modifier.width(1.dp).height(64.dp).background(Color(0xFFBDD6E8)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(36.dp).clip(CircleShape)
                .background(if (isDark) Color(0xFF203C55) else Color(0xFFE0F0FF)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Buka $title", tint = interviewBlue)
            }
        }
    }
}
