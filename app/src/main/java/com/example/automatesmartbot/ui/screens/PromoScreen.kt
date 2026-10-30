package com.example.automatesmartbot.ui.screens

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
            .background(Color(0xFFF5FAFF))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Promo Qpon.id
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFFEC4899))
                        Text(
                            text = "ðŸŽ Promo Eksklusif Qpon.id",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF17324D)
                        )
                    }
                    Text(
                        text = "Gunakan kode kupon resmi Qpon.id di bawah ini untuk mendapatkan potongan harga berlangganan AutoMateSmartBot PRO.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Membership Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Status Anda Saat Ini", fontSize = 13.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isPro) "PRO MEMBER â­" else "FREE MEMBER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isPro) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                            )
                        }

                        Surface(
                            color = if (isPro) Color(0xFF065F46) else Color(0xFF22304A),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isPro) "Status Aktif" else "Belum Upgrade",
                                color = if (isPro) Color(0xFF34D399) else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Daftar Kupon Qpon.id Tersedia",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF17324D)
            )

            // Qpon List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(qponList) { qpon ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF22304A),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = qpon.code,
                                        color = Color(0xFF1678C8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = qpon.discount,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(text = qpon.description, fontSize = 13.sp, color = Color(0xFF17324D))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Berlaku hingga: ${qpon.validUntil}", fontSize = 11.sp, color = Color(0xFF64748B))

                                TextButton(onClick = {
                                    snackbarMessage = "Kode kupon '${qpon.code}' berhasil disalin!"
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1678C8))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salin Kupon", fontSize = 12.sp, color = Color(0xFF1678C8))
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
                        Text("OK", color = Color(0xFF1678C8))
                    }
                },
                containerColor = Color(0xFF22304A),
                contentColor = Color(0xFF17324D)
            ) {
                Text(msg)
            }
        }
    }
}

