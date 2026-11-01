package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
    BackHandler(selected != null) { selected = null }
    val visible = remember(entries, query, onlyFavorites, favorites) {
        entries.filter { entry ->
            val id = entry.optString("id")
            (!onlyFavorites || id in favorites) &&
                (query.isBlank() || listOf("name", "fullName", "definition", "category", "keywords")
                    .any { entry.optString(it).contains(query, ignoreCase = true) })
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("WorkPedia", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF17324D))
            TextButton(onClick = onExit) { Text("Tutup") }
        }
        if (entries.isEmpty()) {
            Text("Data WorkPedia belum terpasang. Periksa aset workpedia_terms.json.", color = MaterialTheme.colorScheme.error)
        } else {
            if (selected == null) {
                OutlinedTextField(query, { query = it }, label = { Text("Cari istilah dunia kerja") }, modifier = Modifier.fillMaxWidth())
                FilterChip(selected = onlyFavorites, onClick = { onlyFavorites = !onlyFavorites }, label = { Text("★ Favorit") })
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visible, key = { it.optString("id") }) { term ->
                        Card(onClick = { selected = term.optString("id") }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(term.optString("name"), style = MaterialTheme.typography.titleMedium)
                                Text(term.optString("fullName"))
                                Text(term.optString("category"), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                val term = entries.firstOrNull { it.optString("id") == selected }
                if (term != null) {
                    TextButton(onClick = { selected = null }) { Text("← Kembali") }
                    Text(term.optString("name"), style = MaterialTheme.typography.headlineSmall)
                    Text(term.optString("fullName"), style = MaterialTheme.typography.titleMedium)
                    listOf("definition" to "Definisi", "simple" to "Penjelasan sederhana", "example" to "Contoh", "rights" to "Hak terkait", "legal" to "Dasar hukum", "source" to "Sumber").forEach { (key, label) ->
                        val value = term.optString(key)
                        if (value.isNotBlank()) { Text(label, style = MaterialTheme.typography.titleSmall); Text(value) }
                    }
                    val id = term.optString("id")
                    Button(onClick = {
                        favorites = if (id in favorites) favorites - id else favorites + id
                        prefs.edit().putStringSet("ids", favorites).apply()
                    }) { Text(if (id in favorites) "★ Hapus Favorit" else "☆ Simpan Favorit") }
                }
            }
        }
    }
}
