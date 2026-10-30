package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.automatesmartbot.R

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.karir_mate_logo),
        contentDescription = "Logo Karir Mate",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
