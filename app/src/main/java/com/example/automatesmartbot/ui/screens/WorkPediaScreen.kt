package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@Composable
fun WorkPediaScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val entries = remember {
        runCatching {
            val json = context.assets.open("workpedia_terms.json").bufferedReader().use { it.readText() }
            JSONArray(json).let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
        }.getOrElse { emptyList() }
    }
    val prefs = remember { context.getSharedPreferences("workpedia_favorites", 0) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("ids", emptySet()).orEmpty().toSet()) }
    var query by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("Semua") }
    val categories = remember(entries) { listOf("Semua") + entries.map { it.optString("category") }.filter { it.isNotBlank() }.distinct().sorted() }
    BackHandler(selected != null) { selected = null }
    val visible = remember(entries, query, onlyFavorites, favorites, category) {
        entries.filter { entry ->
            val id = entry.optString("id")
            (!onlyFavorites || id in favorites) && (category == "Semua" || entry.optString("category") == category) &&
                (query.isBlank() || listOf("name", "fullName", "definition", "category", "keywords")
                    .any { entry.optString(it).contains(query, ignoreCase = true) })
        }
    }
    val navy = Color(0xFF102D49)
    val teal = Color(0xFF008F86)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F8FC)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = navy)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KARIRMATE • PUSAT PENGETAHUAN", color = Color(0xFF65E6D6), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("WorkPedia", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Pahami istilah dunia kerja, hak karyawan, dan regulasi.", color = Color(0xFFD9E6EE))
                Text("${entries.size} istilah  •  ${favorites.size} favorit", color = Color(0xFF65E6D6), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (entries.isEmpty()) {
            Text("Data WorkPedia belum terpasang.", color = MaterialTheme.colorScheme.error)
        } else if (selected == null) {
            OutlinedTextField(query, { query = it }, label = { Text("Cari istilah, singkatan, atau aturan") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), trailingIcon = { if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("×") } })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !onlyFavorites, onClick = { onlyFavorites = false }, label = { Text("Semua") })
                FilterChip(selected = onlyFavorites, onClick = { onlyFavorites = true }, label = { Text("★ Favorit") })
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onExit) { Text("Keluar") }
            }
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories.size) { i ->
                    FilterChip(selected = category == categories[i], onClick = { category = categories[i] }, label = { Text(categories[i]) })
                }
            }
            Text("${visible.size} istilah ditemukan", color = navy, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(visible, key = { it.optString("id") }) { term ->
                    Card(onClick = { selected = term.optString("id") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(term.optString("name"), color = navy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                Text(if (term.optString("id") in favorites) "★" else "→", color = teal)
                            }
                            Text(term.optString("fullName"), color = navy, style = MaterialTheme.typography.bodyMedium)
                            Text(term.optString("category"), color = teal, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            val term = entries.firstOrNull { it.optString("id") == selected }
            if (term != null) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { selected = null }) { Text("← Kembali ke daftar", color = teal) }
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(term.optString("category").uppercase(), color = teal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(term.optString("name"), color = navy, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            Text(term.optString("fullName"), color = navy, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    listOf("definition" to "Definisi", "simple" to "Penjelasan Sederhana", "example" to "Contoh Penerapan", "rights" to "Hak Terkait", "legal" to "Dasar Hukum", "source" to "Sumber").forEach { (key, label) ->
                        val value = term.optString(key)
                        if (value.isNotBlank()) Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(label, color = teal, fontWeight = FontWeight.Bold)
                                Text(value, color = navy)
                            }
                        }
                    }
                    val id = term.optString("id")
                    Button(onClick = {
                        favorites = if (id in favorites) favorites - id else favorites + id
                        prefs.edit().putStringSet("ids", favorites).apply()
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = teal)) {
                        Text(if (id in favorites) "★ Hapus dari Favorit" else "☆ Simpan ke Favorit")
                    }
                }
            }
        }
    }
}
