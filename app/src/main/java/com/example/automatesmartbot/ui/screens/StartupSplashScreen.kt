package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.AccentGold
import com.example.automatesmartbot.ui.theme.FreshMint
import com.example.automatesmartbot.ui.theme.OceanBlue
import com.example.automatesmartbot.ui.theme.TextWhite

/** Splash layout based on the supplied blue/mint KarirMate visual reference. */
@Composable
fun StartupSplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(OceanBlue, androidx.compose.ui.graphics.Color(0xFF064C91))
            )
        )
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter)) {
            val mintWave = Path().apply {
                moveTo(0f, size.height * .65f)
                cubicTo(size.width * .28f, size.height * .55f, size.width * .64f, size.height * .12f, size.width, size.height * .18f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mintWave, Brush.horizontalGradient(listOf(OceanBlue, FreshMint)), style = Fill)
            val blueWave = Path().apply {
                moveTo(0f, size.height * .46f)
                cubicTo(size.width * .34f, size.height * .75f, size.width * .58f, size.height * .3f, size.width, size.height * .85f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(blueWave, androidx.compose.ui.graphics.Color(0xFF07569C))
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Temporary vector mark until the exact standalone logo asset is available.
            Canvas(modifier = Modifier.size(92.dp)) {
                val k = Path().apply {
                    moveTo(size.width * .30f, size.height * .12f)
                    lineTo(size.width * .48f, size.height * .12f)
                    lineTo(size.width * .37f, size.height * .50f)
                    lineTo(size.width * .78f, size.height * .16f)
                    lineTo(size.width * .88f, size.height * .30f)
                    lineTo(size.width * .52f, size.height * .59f)
                    lineTo(size.width * .80f, size.height * .87f)
                    lineTo(size.width * .57f, size.height * .87f)
                    lineTo(size.width * .34f, size.height * .65f)
                    lineTo(size.width * .26f, size.height * .86f)
                    lineTo(size.width * .08f, size.height * .86f)
                    close()
                }
                drawPath(k, Brush.linearGradient(listOf(TextWhite, FreshMint)))
                drawCircle(AccentGold, radius = size.width * .085f, center = Offset(size.width * .89f, size.height * .12f))
            }
            Text(
                text = "KarirMate",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Partner Persiapan Kariermu",
                fontSize = 13.sp,
                color = TextWhite.copy(alpha = .9f),
                textAlign = TextAlign.Center
            )
            CircularProgressIndicator(
                color = FreshMint,
                strokeWidth = 2.dp,
                modifier = Modifier.padding(top = 18.dp).size(22.dp)
            )
        }
    }
}
