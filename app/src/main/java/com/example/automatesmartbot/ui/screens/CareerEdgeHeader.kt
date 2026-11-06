package com.example.automatesmartbot.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.automatesmartbot.R

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Header background extends into Android's *real* transparent status bar.
 * Clock, connectivity, notifications and battery remain system-owned overlays.
 */
@Composable
internal fun CareerEdgeHeader(
    title: String,
    subtitle: String,
    isPro: Boolean,
    onBack: () -> Unit,
    onProfile: () -> Unit
) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val old = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false  // white clock / signal icons over blue
        onDispose { if (old != null) controller.isAppearanceLightStatusBars = old }
    }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF075FAA), Color(0xFF046CAB), Color(0xFF059DA9))))
    ) {
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = .08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .4f))
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali satu tahap", tint = Color.White)
                    }
                }
                Image(
                    painter = painterResource(R.drawable.karirmate_brand_mark),
                    contentDescription = "KarirMate",
                    modifier = Modifier.size(42.dp),
                    contentScale = ContentScale.Fit
                )
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 20.sp, lineHeight = 23.sp, color = Color.White, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    Text(subtitle, fontSize = 11.sp, lineHeight = 14.sp, color = Color.White.copy(alpha = .9f), maxLines = 2)
                }
                com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.HEADER)
                IconButton(onClick = onProfile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Person, contentDescription = "Profil", tint = Color.White, modifier = Modifier.size(23.dp))
                }
            }
        }
    }
}
