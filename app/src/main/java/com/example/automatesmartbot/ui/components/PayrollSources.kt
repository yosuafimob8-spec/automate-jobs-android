package com.example.automatesmartbot.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun PayrollSources() {
    val uri = LocalUriHandler.current
    Text("Dasar estimasi · diperiksa 31 Oktober 2026. Ketentuan khusus dan basis iuran aktual perlu dicocokkan dengan slip gaji.", style = MaterialTheme.typography.bodySmall)
    listOf(
        "Lembur · PP 35/2021" to "https://jdih.kemnaker.go.id/peraturan/detail/1723/peraturan-pemerintah-nomor-35-tahun-2021",
        "THR · Permenaker 6/2016" to "https://peraturan.bpk.go.id/Details/146101/permenaker-no-6-tahun-2016",
        "PPh 21 tahunan · DJP" to "https://kalkulator.pajak.go.id/",
        "Iuran JHT & JP · BPJS Ketenagakerjaan" to "https://www.bpjsketenagakerjaan.go.id/en/penerima-upah.html",
        "Dasar BPJS Kesehatan · Kemenkeu" to "https://anggaran.kemenkeu.go.id/in/post/perpres-64-2020-upaya-pemerintah-perbaiki-sistem-jaminan-kesehatan-nasional"
    ).forEach { (label, url) -> TextButton(onClick = { uri.openUri(url) }) { Text(label) } }
}
