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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.automatesmartbot.R

private fun Context.historyActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.historyActivity()
    else -> null
}

/** Single Riwayat header, including the REAL Android status bar, matching the reference.
 * This is used instead of AppTopBar; never place a second header inside RiwayatScreen.
 */
@Composable
internal fun HistoryBrandedHeader(onBack: () -> Unit) {
    val activity = LocalContext.current.historyActivity()
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF075AA8), Color(0xFF0769B2), Color(0xFF008EAB))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val top = Path().apply {
                moveTo(size.width * .64f, 0f)
                cubicTo(size.width * .70f, size.height * .57f, size.width * .91f, size.height * .37f, size.width, size.height * .84f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(top, Color(0xFF12B2C5).copy(alpha = .28f))
            val bottom = Path().apply {
                moveTo(size.width * .23f, size.height)
                cubicTo(size.width * .15f, size.height * .25f, size.width * .44f, size.height * .34f, size.width * .42f, size.height)
                close()
            }
            drawPath(bottom, Color(0xFF064787).copy(alpha = .20f))
        }
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(91.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = .05f),
                    border = BorderStroke(1.dp, Color(0xFF6CB5E5).copy(alpha = .78f))
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White,
                            modifier = Modifier.size(25.dp))
                    }
                }
                Image(
                    painter = painterResource(R.drawable.karirmate_brand_mark),
                    contentDescription = "Logo KarirMate",
                    modifier = Modifier.size(39.dp),
                    contentScale = ContentScale.Fit
                )
                Box(Modifier.width(1.dp).height(42.dp).background(Color.White.copy(alpha = .45f)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Riwayat Lamaran", fontSize = 17.sp, lineHeight = 21.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text("Lihat dan kelola riwayat pengiriman lamaran", fontSize = 9.7.sp, lineHeight = 12.sp,
                        color = Color.White.copy(alpha = .94f), maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
                com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.HEADER)
            }
        }
    }
}
