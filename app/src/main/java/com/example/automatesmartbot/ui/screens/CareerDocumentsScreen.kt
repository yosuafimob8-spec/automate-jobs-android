package com.example.automatesmartbot.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.automatesmartbot.data.career.*
import com.example.automatesmartbot.ui.components.AppInput
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import kotlinx.coroutines.*
import java.io.File

@Composable
fun CareerDocumentsScreen(onLegacyDocuments: () -> Unit) {
    val context = LocalContext.current
    val store = remember { CareerPdfStore(context) }
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(store.list()) }
    var selected by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var order by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var savePath by rememberSaveable { mutableStateOf<String?>(null) }

    fun operation(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) { block() }
                items = withContext(Dispatchers.IO) { store.list() }
                selected = arrayListOf()
                message = "Dokumen baru tersimpan. Dokumen sumber tetap tersedia."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message ?: "Operasi PDF gagal."
            } finally {
                busy = false
            }
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) operation {
            require(uris.size <= 10) { "Pilih maksimal 10 file." }
            uris.forEach { store.importPdf(it) }
        }
    }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val source = savePath
        if (uri != null && source != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        File(source).inputStream().use { it.copyTo(out) }
                    } ?: error("Lokasi tidak dapat ditulis.")
                }
                message = "PDF berhasil disimpan ke lokasi pilihan."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Penyimpanan gagal: ${e.message}"
            } finally {
                busy = false
            }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            KarirMateSectionHero("Dokumen Karier", "Kelola dan susun dokumen PDF secara aman di perangkat Anda.")

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Perpustakaan PDF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                "${items.size} Dokumen",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        "${selected.size} dokumen dipilih untuk pemrosesan.",
                        color = KarirTextMuted,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = { importer.launch(arrayOf("application/pdf")) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                    ) {
                        Text("+ Tambahkan Dokumen PDF", fontWeight = FontWeight.Bold)
                    }

                    if (selected.isNotEmpty()) {
                        TextButton(onClick = { selected = arrayListOf() }) {
                            Text("Batalkan Semua Pilihan", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            TextButton(onClick = onLegacyDocuments) {
                Text("Buka Surat Izin & Dokumen Kerja →", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
            }

            if (busy) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    color = KarirOrange,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }

            if (message.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(message, color = Color(0xFF9A3412), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            if (items.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Belum ada dokumen PDF tersimpan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Tambahkan PDF dari perangkat Anda untuk mulai menyusun dokumen karier.", fontSize = 12.sp, color = KarirTextMuted)
                    }
                }
            }

            items.forEach { item ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.id in selected,
                                onCheckedChange = { checked ->
                                    selected = ArrayList(if (checked) selected + item.id else selected.filter { it != item.id })
                                },
                                colors = CheckboxDefaults.colors(checkedColor = KarirOrange)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${item.name} • ${item.pages} halaman",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(
                                onClick = { savePath = item.path; exporter.launch(item.name) },
                                enabled = !busy
                            ) {
                                Text("Simpan ke HP", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.career-files", File(item.path))
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Bagikan PDF"))
                                    } catch (e: Exception) {
                                        message = "PDF belum dapat dibagikan: ${e.message}"
                                    }
                                }
                            ) {
                                Text("Bagikan PDF", color = KarirOrange, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            val chosen = items.filter { it.id in selected }
            HorizontalDivider(color = Color(0xFFF1E5DC))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alat Pengolahan PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Pilih dua dokumen untuk menggabungkan, atau satu dokumen untuk menyusun dan memutar halaman.", color = KarirTextMuted, fontSize = 12.sp)

                    Button(
                        onClick = { operation { store.merge(chosen) } },
                        enabled = !busy && chosen.size >= 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                    ) {
                        Text("Gabungkan PDF Terpilih", fontWeight = FontWeight.Bold)
                    }

                    AppInput("Halaman untuk diambil / disusun (contoh: 3,1,2)", order, { order = it })

                    Button(
                        onClick = {
                            operation {
                                val source = chosen.single()
                                val pages = order.split(',').map { it.trim().toIntOrNull() ?: error("Gunakan nomor halaman dipisahkan koma.") }
                                store.transform(source, pages, 0)
                            }
                        },
                        enabled = !busy && chosen.size == 1 && order.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                    ) {
                        Text("Buat PDF dari Urutan Halaman", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            operation {
                                val source = chosen.single()
                                store.transform(source, (1..source.pages).toList(), 90)
                            }
                        },
                        enabled = !busy && chosen.size == 1,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, KarirOrange),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KarirOrange)
                    ) {
                        Text("Putar 90° ke Dokumen Baru", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
