package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SuratIzinDokumenScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Dokumen Karier", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gunakan menu Dokumen Karier untuk mengelola dokumen pekerjaan.")
    }
}
