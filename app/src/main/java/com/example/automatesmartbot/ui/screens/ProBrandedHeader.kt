package com.example.automatesmartbot.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.automatesmartbot.R

private fun Context.proHeaderActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.proHeaderActivity()
    else -> null
}

/** One-and-only header for Paket PRO. Layout is intentionally matched to
 * AI Interview's blue wave header, including the transparent status bar.
 */
@Composable
internal fun ProBrandedHeader(onBack: () -> Unit) {
    val activity = LocalContext.current.proHeaderActivity()
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 29.dp, bottomEnd = 29.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF075AA8), Color(0xFF0769B2), Color(0xFF008EAB))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val topWave = Path().apply {
                moveTo(size.width * .63f, 0f)
                cubicTo(size.width * .70f, size.height * .58f, size.width * .91f, size.height * .36f, size.width, size.height * .83f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(topWave, Color(0xFF14B0C5).copy(alpha = .26f))
            val lowerWave = Path().apply {
                moveTo(size.width * .25f, size.height)
                cubicTo(size.width * .15f, size.height * .27f, size.width * .47f, size.height * .35f, size.width * .41f, size.height)
                close()
            }
            drawPath(lowerWave, Color(0xFF004A92).copy(alpha = .22f))
        }
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(91.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = .05f),
                    border = BorderStroke(1.dp, Color(0xFF69B7E8).copy(alpha = .78f))
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(43.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White,
                            modifier = Modifier.size(26.dp))
                    }
                }
                Image(
                    painter = painterResource(R.drawable.karirmate_brand_mark),
                    contentDescription = "Logo KarirMate",
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit
                )
                Box(Modifier.width(1.dp).height(42.dp).background(Color.White.copy(alpha = .34f)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Paket PRO", fontSize = 21.sp, lineHeight = 24.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                    Text("Pilih paket premium KarirMate", fontSize = 10.5.sp, lineHeight = 13.sp,
                        color = Color.White.copy(alpha = .94f), maxLines = 2)
                }
                com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.HEADER)
            }
        }
    }
}
