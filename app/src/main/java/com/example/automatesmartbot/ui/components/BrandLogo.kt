package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
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
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(listOf(Color(0xFF087DBA), Color(0xFF084A8A), Color(0xFF087E9A))),
            shape = RoundedCornerShape(12.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.karirmate_brand_mark),
            contentDescription = "Logo KarirMate",
            modifier = Modifier.fillMaxSize().padding(3.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/** Brand wordmark: high contrast Karir + mint Mate, never plain black. */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    onDark: Boolean = false
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        Text("Karir", color = if (onDark || MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color(0xFF0B3556), fontWeight = FontWeight.ExtraBold, fontSize = fontSize)
        Text("Mate", color = if (onDark || MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF55E3BF) else Color(0xFF087E72), fontWeight = FontWeight.ExtraBold, fontSize = fontSize)
    }
}
