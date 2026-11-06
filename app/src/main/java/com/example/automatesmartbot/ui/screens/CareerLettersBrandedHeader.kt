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

private fun Context.careerLettersActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.careerLettersActivity()
    else -> null
}

/** One blue, edge-to-edge header replacing the standard AppTopBar on Surat Lamaran & Resign only. */
@Composable
internal fun CareerLettersBrandedHeader(isPro: Boolean, onBack: () -> Unit) {
    val activity = LocalContext.current.careerLettersActivity()
    DisposableEffect(activity) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0557A1), Color(0xFF096AB7), Color(0xFF049BAB))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val upper = Path().apply {
                moveTo(size.width * .64f, 0f)
                cubicTo(size.width * .68f, size.height * .51f, size.width * .91f,
                    size.height * .43f, size.width, size.height * .88f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(upper, Color(0xFF2BC6D6).copy(alpha = .25f))
            val lower = Path().apply {
                moveTo(size.width * .25f, size.height)
                cubicTo(size.width * .17f, size.height * .33f, size.width * .49f,
                    size.height * .42f, size.width * .47f, size.height)
                close()
            }
            drawPath(lower, Color(0xFF064280).copy(alpha = .20f))
        }
        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(91.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = .10f),
                    border = BorderStroke(1.dp, Color(0xFF70BCE9).copy(alpha = .7f))
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(41.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali",
                            tint = Color.White, modifier = Modifier.size(25.dp))
                    }
                }
                Image(
                    painter = painterResource(R.drawable.karirmate_brand_mark),
                    contentDescription = "Logo KarirMate", modifier = Modifier.size(37.dp),
                    contentScale = ContentScale.Fit
                )
                Box(Modifier.width(1.dp).height(41.dp).background(Color.White.copy(alpha = .49f)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Surat Lamaran & Resign", fontSize = 14.sp, lineHeight = 22.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text("Kelola surat lamaran dan dokumen karier", fontSize = 9.sp,
                        lineHeight = 12.sp, color = Color.White.copy(alpha = .92f), maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
                com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.HEADER)
            }
        }
    }
}
