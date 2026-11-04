package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "KARIRMATE • PUSAT PENGETAHUAN",
                            color = KarirOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                "${entries.size} Istilah",
                                color = Color(0xFFE65100),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text("WorkPedia", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Pahami istilah penting dunia kerja, hak normatif karyawan, dan aturan ketenagakerjaan.", color = KarirTextMuted, fontSize = 12.5.sp)
                }
            }

            if (entries.isEmpty()) {
                Text("Data WorkPedia belum terpasang.", color = MaterialTheme.colorScheme.error)
            } else if (selected == null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cari istilah, singkatan, atau aturan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = Color(0xFF9A3412),
                        unfocusedLabelColor = KarirTextMuted,
                        focusedPlaceholderColor = KarirTextMuted,
                        unfocusedPlaceholderColor = KarirTextMuted,
                        focusedBorderColor = KarirOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = KarirOrange,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            TextButton(onClick = { query = "" }) {
                                Text("×", fontSize = 18.sp, color = KarirOrange)
                            }
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !onlyFavorites,
                        onClick = { onlyFavorites = false },
                        label = { Text("Semua", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFF3E0),
                            selectedLabelColor = Color(0xFF9A3412),
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (!onlyFavorites) KarirOrange else MaterialTheme.colorScheme.outlineVariant)
                    )
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = { onlyFavorites = true },
                        label = { Text("★ Favorit (${favorites.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFF3E0),
                            selectedLabelColor = Color(0xFF9A3412),
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (onlyFavorites) KarirOrange else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onExit) {
                        Text("Keluar", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories.size) { i ->
                        val isSelected = category == categories[i]
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = categories[i] },
                            label = { Text(categories[i], fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFF3E0),
                                selectedLabelColor = Color(0xFF9A3412),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) KarirOrange else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                Text(
                    "${visible.size} istilah ditemukan",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                if (visible.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tidak ada istilah yang cocok", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Ubah kata kunci atau kategori untuk melihat istilah lainnya.", color = KarirTextMuted, fontSize = 12.sp)
                            TextButton(onClick = { query = ""; category = "Semua"; onlyFavorites = false }) {
                                Text("Atur Ulang Pencarian", color = KarirOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(visible, key = { it.optString("id") }) { term ->
                        Card(
                            onClick = { selected = term.optString("id") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        term.optString("name"),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = {
                                        val id = term.optString("id")
                                        favorites = if (id in favorites) favorites - id else favorites + id
                                        prefs.edit().putStringSet("ids", favorites).apply()
                                    }) {
                                        Text(
                                            if (term.optString("id") in favorites) "★" else "☆",
                                            color = KarirOrange,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Text(term.optString("fullName"), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF3E0),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        term.optString("category"),
                                        color = Color(0xFFE65100),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val term = entries.firstOrNull { it.optString("id") == selected }
                if (term != null) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { selected = null }) {
                            Text("← Kembali ke daftar", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF3E0),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        term.optString("category").uppercase(),
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(term.optString("name"), color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text(term.optString("fullName"), color = KarirTextMuted, fontSize = 14.sp)
                            }
                        }

                        listOf(
                            "definition" to "Definisi",
                            "simple" to "Penjelasan Sederhana",
                            "example" to "Contoh Penerapan",
                            "rights" to "Hak Terkait",
                            "legal" to "Dasar Hukum",
                            "source" to "Sumber Informasi"
                        ).forEach { (key, label) ->
                            val value = term.optString(key)
                            if (value.isNotBlank()) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(label, color = KarirOrangeDeep, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        val id = term.optString("id")
                        Button(
                            onClick = {
                                favorites = if (id in favorites) favorites - id else favorites + id
                                prefs.edit().putStringSet("ids", favorites).apply()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                        ) {
                            Text(
                                if (id in favorites) "★ Hapus dari Favorit" else "☆ Simpan ke Favorit",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
