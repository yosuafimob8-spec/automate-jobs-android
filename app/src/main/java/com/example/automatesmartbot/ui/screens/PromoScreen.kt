package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

data class QponItem(
    val code: String,
    val discount: String,
    val description: String,
    val validUntil: String
)

@Composable
fun PromoScreen(
    isPro: Boolean
) {
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val qponList = listOf(
        QponItem("QPONBOT50", "Diskon 50%", "Potongan harga spesial upgrade PRO bulanan via Qpon.id", "31 Des 2025"),
        QponItem("AUTOMAVID", "Diskon 30%", "Voucher khusus tools auto upload & bot lamaran kerja", "15 Nov 2025"),
        QponItem("SMARTBOTNEW", "Gratis 7 Hari", "Trial akses PRO penuh tanpa kartu kredit", "30 Okt 2025")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Promo Qpon.id
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFFFF0E5),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = KarirOrange, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = "Promo Eksklusif Qpon.id",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = KarirTextDark
                        )
                    }
                    Text(
                        text = "Gunakan kode kupon resmi Qpon.id di bawah ini untuk mendapatkan potongan harga berlangganan KarirMate PRO.",
                        fontSize = 13.sp,
                        color = KarirTextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            // Membership Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Status Anda Saat Ini", fontSize = 12.sp, color = KarirTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPro) "PRO MEMBER ⭐" else "FREE MEMBER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isPro) KarirOrange else KarirTextDark
                        )
                    }

                    Surface(
                        color = if (isPro) Color(0xFFE8F5E9) else Color(0xFFFFF0E5),
                        border = BorderStroke(1.dp, if (isPro) Color(0xFFA5D6A7) else Color(0xFFFFDEC9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isPro) "Status Aktif" else "Belum Upgrade",
                            color = if (isPro) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Text(
                text = "Daftar Kupon Qpon.id Tersedia",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = KarirTextDark
            )

            // Qpon List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(qponList) { qpon ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFFFF2E5),
                                    border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = qpon.code,
                                        color = KarirOrangeDeep,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = qpon.discount,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = qpon.description,
                                fontSize = 13.sp,
                                color = KarirTextDark,
                                lineHeight = 18.sp
                            )

                            HorizontalDivider(color = Color(0xFFF1E5DC), thickness = 0.75.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Berlaku hingga: ${qpon.validUntil}",
                                    fontSize = 11.5.sp,
                                    color = KarirTextMuted
                                )

                                TextButton(
                                    onClick = {
                                        snackbarMessage = "Kode kupon '${qpon.code}' berhasil disalin!"
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = KarirOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Salin Kupon",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KarirOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        snackbarMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { snackbarMessage = null }) {
                        Text("OK", color = KarirOrange, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = KarirTextDark,
                contentColor = Color.White
            ) {
                Text(msg)
            }
        }
    }
}
