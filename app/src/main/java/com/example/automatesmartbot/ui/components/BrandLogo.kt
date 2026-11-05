package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.R

/** Shared brand mark for headers, navigation and splash. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.karirmate_brand_mark),
        contentDescription = "Logo KarirMate",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/** Brand wordmark: high contrast Karir + mint Mate, never plain black. */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    onDark: Boolean = false
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        Text("Karir", color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = fontSize)
        Text("Mate", color = Color(0xFF16A88C).let { if (onDark) Color(0xFF55E3BF) else it }, fontWeight = FontWeight.ExtraBold, fontSize = fontSize)
    }
}
