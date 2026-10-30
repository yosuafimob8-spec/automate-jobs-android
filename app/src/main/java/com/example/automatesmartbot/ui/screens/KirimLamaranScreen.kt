package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.DarkBackground
import com.example.automatesmartbot.ui.theme.DarkSurface
import com.example.automatesmartbot.ui.theme.ProGold
import com.example.automatesmartbot.ui.theme.TextGray
import com.example.automatesmartbot.ui.theme.TextWhite

private val accent = Color(0xFFFF6428)

@Composable
fun KirimLamaranScreen(
    isPro: Boolean,
    isOnline: Boolean,
    onNavigateProfil: () -> Unit
) {
    var position by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var dailyLimit by remember { mutableStateOf("5") }
    var emailStyle by remember { mutableStateOf("AI Smart Match") }
    var agreed by remember { mutableStateOf(false) }
    val styles = listOf("Standar Profesional", "Menonjolkan Keahlian", "Menonjolkan Pengalaman", "AI Smart Match")

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5FAFF)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Kirim Lamaran Otomatis", color = Color(0xFF17324D), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("AI mencari pekerjaan, mencocokkan CV, dan menyiapkan email lamaran.", color = Color(0xFF64748B), fontSize = 13.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Target Pekerjaan", color = Color(0xFF17324D), fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("Posisi pekerjaan") }, placeholder = { Text("Contoh: Admin Warehouse") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Daerah pencarian") }, placeholder = { Text("Contoh: Tangerang") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dailyLimit, onValueChange = { dailyLimit = it.filter(Char::isDigit).take(2) }, label = { Text("Maksimal email per hari") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("AI juga dapat mempertimbangkan posisi relevan dengan pengalaman di CV.", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. CV & Profil", color = Color(0xFF17324D), fontWeight = FontWeight.Bold)
                    Text("CV dan informasi kontak akan digunakan untuk mencocokkan posisi dan menyusun lamaran. Integrasi pembacaan CV belum terhubung di layar ini.", color = Color(0xFF64748B), fontSize = 12.sp)
                    TextButton(onClick = onNavigateProfil) { Text("Buka Profil / CV", color = ProGold) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("3. Gaya Email", color = Color(0xFF17324D), fontWeight = FontWeight.Bold)
                    styles.forEach { style ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = emailStyle == style, onClick = { emailStyle = style }, colors = RadioButtonDefaults.colors(selectedColor = accent))
                            Text(style, color = Color(0xFF17324D), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFD8E9F5))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("4. Aktivasi Pengiriman", color = Color(0xFF17324D), fontWeight = FontWeight.Bold)
                    Text(if (isPro) "Akun PRO" else "Fitur otomatis khusus PRO", color = ProGold, fontWeight = FontWeight.SemiBold)
                    Text("Hanya email rekrutmen terverifikasi. Lowongan dan lamaran spontan dibedakan; tidak mengirim duplikat. Pengiriman memerlukan izin pengguna dan integrasi backend.", color = Color(0xFF64748B), fontSize = 12.sp)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = agreed, onCheckedChange = { agreed = it }, colors = CheckboxDefaults.colors(checkedColor = accent))
                        Text("Saya menyetujui aturan pengiriman di atas", color = Color(0xFF17324D), fontSize = 12.sp)
                    }
                    if (!isOnline) Text("Offline: sambungkan internet untuk menjalankan pencarian.", color = Color(0xFFF87171), fontSize = 12.sp)
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFD8E9F5), disabledContentColor = Color(0xFF64748B)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("Aktivasi menunggu integrasi backend") }
                    if (!isPro) TextButton(onClick = onNavigateProfil) { Text("Lihat status PRO di Profil", color = ProGold) }
                }
            }
        }
        item {
            Text("Ini adalah antarmuka konfigurasi. Belum ada pencarian atau email yang dijalankan dari layar ini.", color = Color(0xFF64748B), fontSize = 11.sp)
        }
    }
}
