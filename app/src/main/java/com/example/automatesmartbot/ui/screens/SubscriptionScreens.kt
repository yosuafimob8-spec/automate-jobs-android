package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.subscription.*
import com.example.automatesmartbot.ui.theme.karirButtonColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private fun price(plan: DemoPlan) = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(plan.rupiah).let { "Rp$it" }
private fun date(time: Long) = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date(time))

@Composable
fun ProCheckoutScreen(planIndex: Int, onComplete: () -> Unit, store: DemoSubscriptionStore? = null) {
    val context = LocalContext.current
    val subscription = remember(store) { store ?: DemoSubscriptionStore(context) }
    var identityReady by remember(store) { mutableStateOf(store != null || com.example.automatesmartbot.data.cv.cvWorkspaceBound(context, com.example.automatesmartbot.data.network.ApiClient.getSessionManager()?.getValidToken())) }
    LaunchedEffect(store) {
        if (store == null) identityReady = runCatching { com.example.automatesmartbot.data.cv.ensureCvWorkspace(context) }.getOrDefault(false)
    }
    val plan = DemoPlans.all.getOrNull(planIndex)
    var confirmed by rememberSaveable(planIndex) { mutableStateOf(false) }
    val requestId = rememberSaveable(planIndex) { UUID.randomUUID().toString() }
    var activated by rememberSaveable(planIndex) { mutableStateOf(false) }
    var expiry by rememberSaveable(planIndex) { mutableLongStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Pembelian Paket", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (plan == null) {
            Text("Paket tidak valid. Kembali ke daftar paket.", color = MaterialTheme.colorScheme.error)
        } else {
            Text("Paket ${plan.name} — ${price(plan)}")
            Text("Masa aktif: ${if (plan.days == 1) "24 jam" else "${plan.days} hari"}")
            Text("SIMULASI: tidak ada pembayaran sungguhan. Aktivasi ini tidak membuka akses server atau template PRO terverifikasi.")
            Row {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it }, enabled = !activated)
                Text("Saya mengonfirmasi aktivasi simulasi paket ${plan.name}.", Modifier.weight(1f))
            }
            Button(onClick = {
                try {
                    val result = subscription.activate(plan.id, requestId)
                    expiry = result.expiresAt; activated = true; error = null
                } catch (e: Exception) { error = e.message ?: "Aktivasi belum tersimpan." }
            }, enabled = identityReady && confirmed && !activated, colors = karirButtonColors()) { Text("Aktifkan PRO Demo") }
            if (!identityReady) Text("Identitas akun belum terverifikasi. Backend diperlukan saat pertama kali menghubungkan akun.", color = MaterialTheme.colorScheme.error)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (activated) {
                Text("Aktivasi demo tersimpan. Berakhir: ${date(expiry)}. Hak akses PRO server tidak berubah.")
                Button(onClick = onComplete, colors = karirButtonColors()) { Text("Kembali ke Paket PRO") }
            }
        }
    }
}
