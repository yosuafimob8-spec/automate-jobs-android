package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

private val proOrange = Color(0xFFFF6B2B)
private data class ProPlan(val name: String, val price: String, val days: Int)
private val proPlans = listOf(ProPlan("Harian", "Rp5.000", 1), ProPlan("Mingguan", "Rp25.000", 7), ProPlan("Bulanan", "Rp79.000", 30), ProPlan("Tahunan", "Rp499.000", 365))

/** Device-only testing. Never use this as authorization for server-side PRO features. */
private class DemoProStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("karirmate_pro_demo", 0)
    fun expiry(): Long = prefs.getLong("expires_at", 0L)
    fun activate(days: Int) {
        val start = maxOf(System.currentTimeMillis(), expiry())
        prefs.edit().putLong("expires_at", start + TimeUnit.DAYS.toMillis(days.toLong())).apply()
    }
}

@Composable
fun UpgradeProScreen(onCheckout: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Upgrade KarirMate PRO", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Semua paket membuka manfaat PRO yang sama. Hanya masa aktif yang berbeda.")
        proPlans.forEachIndexed { index, plan ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PRO ${plan.name}", fontWeight = FontWeight.Bold)
                    Text("${plan.price} / ${if (plan.days == 1) "24 jam" else "${plan.days} hari"}")
                    Button(onClick = { onCheckout(index) }, colors = ButtonDefaults.buttonColors(containerColor = proOrange)) { Text("Pilih paket") }
                }
            }
        }
        Text("Mode pengujian. Belum ada transaksi uang sungguhan.")
    }
}

@Composable
fun ProCheckoutScreen(planIndex: Int, onComplete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val plan = proPlans.getOrElse(planIndex) { proPlans.first() }
    var activated by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Checkout PRO", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Paket ${plan.name} — ${plan.price}")
        Text("Masa aktif: ${if (plan.days == 1) "24 jam" else "${plan.days} hari"}")
        Text("SIMULASI: tidak ada pembayaran sungguhan.")
        Button(onClick = { DemoProStore(context).activate(plan.days); activated = true }, enabled = !activated, colors = ButtonDefaults.buttonColors(containerColor = proOrange)) { Text("Aktifkan PRO Demo") }
        if (activated) {
            Text("Aktivasi demo berhasil. Hak akses PRO server tidak berubah.")
            Button(onClick = onComplete) { Text("Kelola Langganan") }
        }
    }
}

@Composable
fun ManageProSubscriptionScreen(onUpgrade: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val expiry = remember { DemoProStore(context).expiry() }
    val active = expiry > System.currentTimeMillis()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Kelola Langganan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (active) "PRO DEMO aktif" else "Tidak ada PRO DEMO aktif")
        if (active) Text("Berakhir: " + java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(expiry)))
        Text("Status ini hanya untuk uji antarmuka. Status PRO asli tetap berasal dari server.")
        Button(onClick = onUpgrade, colors = ButtonDefaults.buttonColors(containerColor = proOrange)) { Text("Lihat paket / Perpanjang") }
    }
}
