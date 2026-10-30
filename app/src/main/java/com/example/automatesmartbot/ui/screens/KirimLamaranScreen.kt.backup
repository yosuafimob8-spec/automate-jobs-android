package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.DarkBackground
import com.example.automatesmartbot.ui.theme.DarkSurface
import com.example.automatesmartbot.ui.theme.DarkSurfaceVariant
import com.example.automatesmartbot.ui.theme.ProGold
import com.example.automatesmartbot.ui.theme.TextGray
import com.example.automatesmartbot.ui.theme.TextWhite

private val sendAccent = Color(0xFF22C55E)

@Composable
fun KirimLamaranScreen(
    isPro: Boolean,
    isOnline: Boolean,
    onNavigateProfil: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = sendAccent.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, sendAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(sendAccent.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = sendAccent,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Kirim Lamaran",
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                color = TextWhite
                            )
                            Text(
                                text = "Siapkan lamaran terbaik Anda",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                    Text(
                        text = "Lengkapi profil dan pilih CV sebelum mengirim lamaran ke HRD.",
                        fontSize = 13.sp,
                        color = TextGray,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPro) Color(0xFF422006) else DarkSurface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isPro) ProGold.copy(alpha = 0.5f) else Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = ProGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isPro) "Akun PRO aktif" else "Fitur khusus PRO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextWhite
                        )
                    }
                    Text(
                        text = if (isPro) {
                            "Pengiriman lamaran akan tersedia setelah fitur ini selesai disiapkan."
                        } else {
                            "Akun Anda masih FREE. Pengiriman lamaran memerlukan status PRO."
                        },
                        fontSize = 12.sp,
                        color = if (isPro) Color(0xFFFDE68A) else TextGray,
                        lineHeight = 18.sp
                    )
                    if (!isPro) {
                        Button(
                            onClick = onNavigateProfil,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Cek Status di Profil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Text(
                        text = "Yang perlu disiapkan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextWhite
                    )
                    listOf(
                        "Profil dan data kontak yang lengkap",
                        "CV berformat PDF maksimal 2 MB",
                        "Email HRD tujuan",
                        "Konfirmasi sebelum lamaran dikirim"
                    ).forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = sendAccent,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(15.dp)
                            )
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                color = TextGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        if (!isOnline) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1D1D)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Anda sedang offline. Sambungkan internet sebelum melanjutkan.",
                            fontSize = 11.sp,
                            color = Color(0xFFFECACA),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengiriman Belum Tersedia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Aplikasi tidak menyimpan kredensial email dan tidak menampilkan log pengiriman palsu.",
                    fontSize = 10.sp,
                    color = TextGray,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
