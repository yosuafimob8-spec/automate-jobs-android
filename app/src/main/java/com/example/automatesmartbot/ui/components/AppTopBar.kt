package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.ui.theme.karirButtonColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.ScreenText as KarirTextDark
import com.example.automatesmartbot.ui.theme.ScreenMuted as KarirNavIconUnselected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    isPro: Boolean,
    onNavigateProfil: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = Modifier.border(width = 0.75.dp, color = screenCanvas(Color(0xFFF1E5DC))),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = KarirTextDark
                    )
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrandLogo(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                if (title == "KarirMate") {
                    BrandWordmark(fontSize = 18.sp)
                } else {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = KarirTextDark
                    )
                }
            }
        },
        actions = {
            com.example.automatesmartbot.ui.components.MembershipBadge(
                style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.LIGHT
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Profile Button
            IconButton(onClick = onNavigateProfil) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profil",
                    tint = KarirNavIconUnselected
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = screenCanvas(Color(0xFFFFFDF9)),
            titleContentColor = KarirTextDark
        )
    )
}
