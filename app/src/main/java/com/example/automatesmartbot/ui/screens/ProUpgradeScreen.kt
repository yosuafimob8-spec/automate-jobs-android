package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.R
import com.example.automatesmartbot.data.subscription.DemoPlan
import com.example.automatesmartbot.data.subscription.DemoPlans
import java.text.NumberFormat
import java.util.Locale

private val ProInk = Color(0xFF142039)
private val ProBlue = Color(0xFF0869B5)
private val ProAzure = Color(0xFF0A91DC)
private val ProGold = Color(0xFFFFA300)
private val ProBorder = Color(0xFFC8E0EF)

/** Upgrade-only page. The sole navigation/header row lives in ProBrandedHeader,
 *  provided by MainDashboardScreen's Scaffold. Never repeat a brand row here.
 */
@Composable
fun UpgradeProScreen(onCheckout: (Int) -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val ink = if (dark) Color.White else ProInk
    val muted = if (dark) Color(0xFFC1D3E2) else Color(0xFF33445B)
    val panel = if (dark) Color(0xFF15283D) else Color(0xFFFCFEFF)
    val bg = if (dark) listOf(Color(0xFF0C2134), Color(0xFF17334A))
             else listOf(Color(0xFFEAF7FF), Color(0xFFF8FCFF), Color(0xFFEAF7FF))

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(bg))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        ProHeroBanner()
        ProSectionHeading(icon = Icons.Default.WorkspacePremium, title = "Keuntungan PRO", color = ProBlue, ink = ink)
        ProBenefitsCard(dark = dark, ink = ink, muted = muted, panel = panel)
        Spacer(Modifier.height(2.dp))
        ProSectionHeading(icon = Icons.Default.Layers, title = "Pilih Paket PRO", color = ProBlue, ink = ink)
        DemoPlans.all.forEachIndexed { index, plan ->
            ProPackageCard(
                plan = plan,
                selectedPopular = plan.id == "monthly",
                dark = dark,
                ink = ink,
                muted = muted,
                onSelect = { onCheckout(index) }
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (dark) Color(0xFF1D344B) else Color(0xFFF0F7FD),
            border = BorderStroke(1.dp, if (dark) Color(0xFF426480) else ProBorder)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1176B8), modifier = Modifier.size(22.dp))
                Text(
                    "Tidak ada transaksi uang sungguhan atau pemotongan saldo.",
                    color = ink, fontSize = 12.sp, lineHeight = 17.sp
                )
            }
        }
        Spacer(Modifier.height(9.dp))
    }
}

@Composable
private fun ProHeroBanner() {
    Box(
        modifier = Modifier.fillMaxWidth().height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0758A7), Color(0xFF0878C4), Color(0xFF0595D8))))
    ) {
        Image(
            painter = painterResource(R.drawable.karirmate_pro_hero_art),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(172.dp)
        )
        // Protect title contrast without making the raster art part of the text.
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(.73f)
                .background(Brush.horizontalGradient(listOf(Color(0xFF0754A0), Color(0xFF0758A7).copy(alpha = .78f), Color.Transparent)))
        )
        Column(
            Modifier.align(Alignment.CenterStart).fillMaxWidth(.64f).padding(start = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("👑 Upgrade ke", fontSize = 16.sp, lineHeight = 19.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("KarirMate PRO", fontSize = 19.sp, lineHeight = 21.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(
                "Akses fitur lengkap untuk persiapan karier yang lebih maksimal.",
                fontSize = 11.sp, lineHeight = 14.sp, color = Color.White
            )
        }
    }
}

@Composable
private fun ProSectionHeading(icon: ImageVector, title: String, color: Color, ink: Color) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(23.dp))
        Text(title, fontSize = 17.5.sp, fontWeight = FontWeight.ExtraBold, color = ink)
    }
}

@Composable
private fun ProBenefitsCard(dark: Boolean, ink: Color, muted: Color, panel: Color) {
    val border = if (dark) Color(0xFF365773) else ProBorder
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
        color = panel, border = BorderStroke(1.dp, border),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    ProBenefit(Icons.Default.TrendingUp, "Akses fitur premium", "(semua modul)", ProBlue, dark, ink, muted)
                }
                Box(Modifier.weight(1f)) {
                    ProBenefit(Icons.Default.BarChart, "Simulasi lengkap", "dan tanpa batas", ProAzure, dark, ink, muted)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 7.dp), color = border)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    ProBenefit(Icons.Default.WorkspacePremium, "Sertifikat resmi", "KarirMate", ProBlue, dark, ink, muted)
                }
                Box(Modifier.weight(1f)) {
                    ProBenefit(Icons.Default.Star, "Prioritas update", "fitur baru", Color(0xFFFFB000), dark, ink, muted)
                }
            }
        }
    }
}

@Composable
private fun ProBenefit(
    icon: ImageVector,
    title: String,
    secondLine: String,
    iconTint: Color,
    dark: Boolean,
    ink: Color,
    muted: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
                .background(if (dark) Color(0xFF254259) else Color(0xFFE6F7FD))
                .border(1.dp, if (dark) Color(0xFF3F6683) else Color(0xFFC8E8F2), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp)) }
        Column {
            Text(title, fontSize = 11.6.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, color = ink)
            Text(secondLine, fontSize = 11.6.sp, lineHeight = 15.sp, color = muted)
        }
    }
}

@Composable
private fun ProPackageCard(
    plan: DemoPlan,
    selectedPopular: Boolean,
    dark: Boolean,
    ink: Color,
    muted: Color,
    onSelect: () -> Unit
) {
    val theme = when (plan.id) {
        "daily" -> Triple(Color(0xFF168FD0), Color(0xFFE5F6FF), Color(0xFFB5DDEF))
        "weekly" -> Triple(Color(0xFF0BB16B), Color(0xFFE7F9F0), Color(0xFFB7E1CF))
        "monthly" -> Triple(Color(0xFFFF9D00), Color(0xFFFFF7E7), Color(0xFFFFB115))
        else -> Triple(Color(0xFF8747D7), Color(0xFFF3EEFF), Color(0xFFD5C5F1))
    }
    val shape = RoundedCornerShape(15.dp)
    val border = if (selectedPopular) 1.5.dp else 1.dp
    val backgroundColors = if (dark) listOf(Color(0xFF173047), Color(0xFF1B3145))
        else listOf(Color(0xFFFFFFFF), theme.second)
    Box(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(backgroundColors))
            .border(border, theme.third, shape)
            .padding(horizontal = 10.dp, vertical = if (selectedPopular) 9.dp else 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (dark) Color(0xFF30475B) else theme.second),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = theme.first, modifier = Modifier.size(28.dp))
                    // The reference has 7/30/365 written inside the calendar icon.
                    if (plan.days != 1) Text(
                        plan.days.toString(), fontSize = 10.sp, lineHeight = 10.sp,
                        color = theme.first, fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("PRO ${plan.name}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Akses penuh selama ${if (plan.days == 1) "24 jam" else "${plan.days} hari"}",
                    fontSize = 10.4.sp, lineHeight = 13.sp, color = muted, maxLines = 2
                )
                val formatted = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(plan.rupiah)
                Text(
                    "Rp$formatted / ${if (plan.days == 1) "24 jam" else "${plan.days} hari"}",
                    fontSize = 13.1.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = ink,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (selectedPopular) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFA10B)) {
                        Text("★ Paling Populer", Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            fontSize = 9.4.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                val modifier = Modifier.width(99.dp).height(32.dp)
                    .semantics { contentDescription = "Pilih paket ${plan.name}" }
                if (selectedPopular) {
                    Button(
                        onClick = onSelect,
                        modifier = modifier,
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0679C1)),
                        shape = RoundedCornerShape(50)
                    ) { Text("Pilih Paket", fontSize = 11.5.sp, maxLines = 1) }
                } else {
                    OutlinedButton(
                        onClick = onSelect,
                        modifier = modifier,
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        border = BorderStroke(1.dp, Color(0xFF136EBC)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (dark) Color(0xFF1A3248) else Color.Transparent,
                            contentColor = if (dark) Color.White else Color(0xFF145B9A)
                        ),
                        shape = RoundedCornerShape(50)
                    ) { Text("Pilih Paket", fontSize = 11.5.sp, maxLines = 1) }
                }
            }
        }
    }
}
