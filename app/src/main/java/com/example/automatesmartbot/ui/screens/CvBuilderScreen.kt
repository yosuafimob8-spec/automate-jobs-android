package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.components.*
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CvBuilderScreen(viewModel: CvBuilderViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val history by viewModel.history.collectAsState()
    val page = history.last()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportBusy by remember { mutableStateOf(false) }
    var exportSnapshot by rememberSaveable { mutableStateOf<String?>(null) }
    var templatePreview by rememberSaveable { mutableStateOf("ats_01") }
    var letterType by rememberSaveable { mutableStateOf(LetterType.STANDARD.name) }
    var jobTitle by rememberSaveable { mutableStateOf("") }
    var company by rememberSaveable { mutableStateOf("") }
    var useAnalyzed by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("Semua") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importPhoto) }
    val pdfInput = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::analyzePdf) }
    val pdfOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val snapshot = exportSnapshot
        if (uri == null) { snapshot?.let { java.io.File(it).delete() }; exportSnapshot = null }
        if (uri != null && snapshot != null) scope.launch {
            exportBusy = true
            try {
                withContext(Dispatchers.IO) {
                    val type = object : com.google.gson.reflect.TypeToken<List<CvPage>>() {}.type
                    val pages: List<CvPage> = com.google.gson.Gson().fromJson(java.io.File(snapshot).readText(), type)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { CvRenderer.writePdf(context, pages, it) } ?: error("Lokasi tidak dapat ditulis.")
                }
                viewModel.message("PDF berhasil disimpan. Buka melalui aplikasi File Android.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { viewModel.message("Ekspor PDF gagal: ${e.message}") }
            finally { exportBusy = false; java.io.File(snapshot).delete(); exportSnapshot = null }
        }
    }
    val docxOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri ->
        if (uri != null) scope.launch {
            exportBusy = true
            try { withContext(Dispatchers.IO) { CvExporter.exportDocx(context, uri, state.draft) }; viewModel.message("DOCX berhasil disimpan.") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { viewModel.message("Ekspor DOCX gagal: ${e.message}") }
            finally { exportBusy = false }
        }
    }
    fun nav(destination: String) = viewModel.navigate(destination)
    fun export(pages: List<CvPage>, filename: String) {
        try {
            val file = java.io.File.createTempFile("cv-export-", ".json", context.cacheDir)
            file.writeText(com.google.gson.Gson().toJson(pages))
            exportSnapshot = file.path
            pdfOutput.launch(filename)
        } catch (e: Exception) { viewModel.message("Ekspor belum dapat disiapkan: ${e.message}") }
    }
    if (deleteId != null) AlertDialog(
        onDismissRequest = { deleteId = null }, title = { Text("Hapus CV?") }, text = { Text("CV di perangkat ini akan dihapus.") },
        confirmButton = { TextButton(onClick = { deleteId?.let(viewModel::deleteDocument); deleteId = null }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KarirMateSectionHero("CV Builder", "Buat CV profesional, pilih template, dan analisis hasilnya.")
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (exportBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        when (page) {
            "CV Builder" -> {
                Text("Buat, kelola, dan analisis CV", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { viewModel.newDocument(); if (viewModel.state.value.activeId != null) nav("CV Saya") }, Modifier.fillMaxWidth()) { Text("Buat CV Baru") }
                OutlinedButton(onClick = { nav("Template") }, Modifier.fillMaxWidth()) { Text("Katalog Template") }
                OutlinedButton(onClick = { nav("CV Analyst") }, Modifier.fillMaxWidth()) { Text("CV Analyst") }
                state.drafts.forEach { document ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(document.content.contact.fullName.ifBlank { document.name }, fontWeight = FontWeight.Bold)
                            Text(CvTemplates.resolve(document.templateId).name)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.selectDocument(document.id); nav("CV Saya") }) { Text("Edit") }
                                TextButton(onClick = { viewModel.selectDocument(document.id); nav("Preview CV") }) { Text("Preview") }
                                TextButton(onClick = { deleteId = document.id }) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
            "CV Saya" -> {
                Text("Perubahan disimpan otomatis di perangkat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("Informasi Pribadi", "Pendidikan", "Pengalaman", "Keterampilan", "Objektif / Ringkasan", "Detail Proyek", "Sertifikasi", "Referensi", "Tambahkan Bagian Lainnya", "Ubah Urutan / Edit Judul").forEach { title ->
                    OutlinedButton(onClick = { nav(title) }, Modifier.fillMaxWidth()) { Text(title) }
                }
                Button(onClick = { if (viewModel.saveDocument()) nav("Preview CV") }, Modifier.fillMaxWidth()) { Text("Simpan & Preview CV") }
                TextButton(onClick = { nav("Template") }) { Text("Pilih Template") }
            }
            "Informasi Pribadi" -> {
                val contact = state.draft.contact
                val bitmap by produceState<android.graphics.Bitmap?>(null, contact.photoPath) {
                    value = withContext(Dispatchers.IO) { android.graphics.BitmapFactory.decodeFile(contact.photoPath) }
                }
                bitmap?.let { Image(it.asImageBitmap(), "Foto CV", Modifier.size(100.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                Text("Foto opsional. Template ATS tidak menggunakan foto.")
                Row {
                    TextButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, enabled = !state.photoLoading) { Text(if (contact.photoPath.isBlank()) "Pilih Foto" else "Ganti Foto") }
                    if (contact.photoPath.isNotBlank()) TextButton(onClick = viewModel::removePhoto) { Text("Hapus Foto") }
                }
                if (state.photoLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                fun update(c: CvContact) = viewModel.updateContent { it.copy(contact = c) }
                AppInput("Nama lengkap", contact.fullName, { update(contact.copy(fullName = it)) })
                AppInput("Posisi / profesi", contact.headline, { update(contact.copy(headline = it)) })
                AppInput("Email", contact.email, { update(contact.copy(email = it)) })
                AppInput("Nomor telepon", contact.phone, { update(contact.copy(phone = it)) })
                AppInput("Domisili", contact.location, { update(contact.copy(location = it)) })
                AppInput("Website", contact.website, { update(contact.copy(website = it)) })
                AppInput("LinkedIn", contact.linkedin, { update(contact.copy(linkedin = it)) })
                AppInput("GitHub", contact.github, { update(contact.copy(github = it)) })
            }
            "Objektif / Ringkasan" -> AppInput("Ringkasan profil", state.draft.summary, { value -> viewModel.updateContent { it.copy(summary = value) } }, minLines = 6)
            "Pendidikan", "Pengalaman", "Keterampilan", "Detail Proyek", "Sertifikasi" -> CvEntriesEditor(page, state.draft, viewModel::updateContent)
            "Referensi", "Tambahkan Bagian Lainnya" -> {
                val entries = state.draft.extras.filter { if (page == "Referensi") it.id == "references" else it.id != "references" }
                entries.forEach { item ->
                    AppInput("Judul bagian", item.title, { value -> viewModel.updateContent { c -> c.copy(extras = c.extras.map { if (it.id == item.id) it.copy(title = value) else it }) } })
                    AppInput("Isi bagian", item.text, { value -> viewModel.updateContent { c -> c.copy(extras = c.extras.map { if (it.id == item.id) it.copy(text = value) else it }) } }, minLines = 4)
                    TextButton(onClick = { viewModel.updateContent { c -> c.copy(extras = c.extras.filter { it.id != item.id }) } }) { Text("Hapus bagian") }
                }
                if (page != "Referensi" || entries.isEmpty()) Button(onClick = { viewModel.updateContent { it.copy(extras = it.extras + CvExtraSection(if (page == "Referensi") "references" else newCvId(), if (page == "Referensi") "Referensi" else "Bahasa / Organisasi / Penghargaan")) } }) { Text("Tambah bagian") }
            }
            "Ubah Urutan / Edit Judul" -> {
                Text("Atur urutan bagian. Judul bagian tambahan dapat diedit pada formulirnya. Template akademik, keterampilan, dan portofolio mendahulukan bagian utamanya.")
                val order = (state.draft.sectionOrder + state.draft.extras.map { it.id }).distinct()
                order.forEachIndexed { index, id ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(id, Modifier.weight(1f).padding(top = 12.dp))
                        TextButton(enabled = index > 0, onClick = { val next = order.toMutableList(); java.util.Collections.swap(next, index, index - 1); viewModel.updateContent { it.copy(sectionOrder = next) } }) { Text("Naik") }
                        TextButton(enabled = index < order.lastIndex, onClick = { val next = order.toMutableList(); java.util.Collections.swap(next, index, index + 1); viewModel.updateContent { it.copy(sectionOrder = next) } }) { Text("Turun") }
                    }
                }
            }
            "Template" -> {
                Text("14 desain dengan susunan nyata. Thumbnail, preview, dan PDF memakai renderer yang sama.")
                ScrollableTabRow(selectedTabIndex = (listOf("Semua") + CvTemplates.all.map { it.category }.distinct()).indexOf(filter).coerceAtLeast(0)) {
                    (listOf("Semua") + CvTemplates.all.map { it.category }.distinct()).forEach { category -> Tab(selected = filter == category, onClick = { filter = category }, text = { Text(category) }) }
                }
                CvTemplates.all.filter { filter == "Semua" || it.category == filter }.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { template ->
                            Card(Modifier.weight(1f).clickable { templatePreview = template.id; nav("Preview Template") }) {
                                Column(Modifier.padding(8.dp)) {
                                    CvPageImage(remember(template.id) { CvRenderer.layout(CvTemplates.sample, template.id).first() }, 1, Modifier.fillMaxWidth(), 360)
                                    Text(template.name, fontWeight = FontWeight.Bold)
                                    Text(template.category, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            "Preview Template", "Preview CV" -> {
                val id = if (page == "Preview Template") templatePreview else state.templateId
                val empty = !state.draft.hasUserText()
                val content = if (empty) CvTemplates.sample else state.draft
                if (empty) Text("Data contoh untuk preview; isi CV Anda sebelum ekspor.")
                Text(CvTemplates.resolve(id).name, fontWeight = FontWeight.Bold)
                val pages = remember(content, id) { CvRenderer.layout(content, id) }
                CvDocumentPreview(pages, Modifier.fillMaxWidth().height(590.dp))
                if (page == "Preview Template") Button(onClick = { viewModel.setTemplate(id); nav("CV Saya") }, Modifier.fillMaxWidth()) { Text("Gunakan Template") }
                else {
                    Button(onClick = { export(CvRenderer.layout(state.draft, id), "KarirMate-CV.pdf") }, enabled = !exportBusy && !empty, modifier = Modifier.fillMaxWidth()) { Text("Ekspor PDF") }
                    OutlinedButton(onClick = { docxOutput.launch("KarirMate-CV.docx") }, enabled = !exportBusy && !empty, modifier = Modifier.fillMaxWidth()) { Text("Ekspor DOCX") }
                    TextButton(onClick = { nav("CV Analyst") }) { Text("Analisis CV") }
                }
            }
            "CV Analyst" -> {
                Text("Analisis struktur dan kelengkapan berdasarkan teks PDF. Skor merupakan pemeriksaan aturan lokal, bukan penilaian perekrut atau jaminan lolos ATS.")
                Button(onClick = { pdfInput.launch(arrayOf("application/pdf")) }, enabled = !state.isAnalyzing, modifier = Modifier.fillMaxWidth()) { Text("Pilih PDF & Analisis") }
                if (state.isAnalyzing) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.analysisError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(onClick = viewModel::runDraftCheck, Modifier.fillMaxWidth()) { Text("Periksa CV Saya") }
                state.ats?.let { report -> Text("Skor kelengkapan CV Saya: ${report.score}/100"); report.issues.forEach { Text("• ${it.title}: ${it.detail}") } }
                Button(onClick = { useAnalyzed = false; nav("Pilih Jenis Surat") }, Modifier.fillMaxWidth()) { Text("Buat Surat Lamaran") }
                Text("Riwayat analisis", fontWeight = FontWeight.Bold)
                state.analyses.forEach { a -> TextButton(onClick = { viewModel.selectAnalysis(a) }) { Text("${a.source} • ${a.score}/100") } }
                Text("Surat tersimpan", fontWeight = FontWeight.Bold)
                state.letters.forEach { letter -> TextButton(onClick = { viewModel.selectLetter(letter); jobTitle = letter.jobTitle; company = letter.company; nav("Editor Surat") }) { Text("${letter.jobTitle} — ${letter.company}") } }
            }
            "Hasil Analisis" -> state.analysis?.let { report ->
                Text("Skor kelengkapan: ${report.score}/100", style = MaterialTheme.typography.headlineMedium)
                Text("Kelebihan", fontWeight = FontWeight.Bold); report.strengths.forEach { Text("• $it") }
                Text("Kekurangan", fontWeight = FontWeight.Bold); report.weaknesses.forEach { Text("• $it") }
                Text("Rekomendasi", fontWeight = FontWeight.Bold); report.recommendations.forEach { Text("• $it") }
                Button(onClick = { useAnalyzed = true; nav("Pilih Jenis Surat") }, Modifier.fillMaxWidth()) { Text("Buat Surat Lamaran") }
                Text("Teks yang berhasil dibaca", fontWeight = FontWeight.Bold)
                Text(report.extractedText)
            }
            "Pilih Jenis Surat" -> {
                LetterType.entries.forEach { type ->
                    OutlinedButton(onClick = { letterType = type.name; nav("Tujuan Surat") }, Modifier.fillMaxWidth()) { Text(type.label) }
                }
            }
            "Tujuan Surat" -> {
                val type = LetterType.valueOf(letterType)
                Text(type.label, fontWeight = FontWeight.Bold)
                if (type == LetterType.AI) Text("Integrasi AI belum dikonfigurasi. Generator AI belum tersedia; pilih jenis surat lain melalui panah Kembali.")
                else {
                    Text(if (useAnalyzed) "Sumber: teks PDF yang dianalisis. Periksa kembali nama dan fakta pada editor." else "Sumber: CV Saya yang aktif.")
                    AppInput("Perusahaan tujuan", company, { company = it })
                    AppInput("Posisi tujuan", jobTitle, { jobTitle = it })
                    Button(onClick = { if (viewModel.generateCoverLetter(jobTitle, company, type, useAnalyzed)) nav("Editor Surat") }, enabled = company.isNotBlank() && jobTitle.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Buat Draf Surat") }
                }
            }
            "Editor Surat" -> state.coverLetter?.let { letter ->
                Text("Surat disimpan otomatis. Periksa dan sunting fakta sebelum digunakan.")
                AppInput("Isi surat", letter.content, viewModel::updateLetter, minLines = 16)
                Button(onClick = { nav("Preview Surat") }, enabled = letter.content.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Preview Surat") }
            }
            "Preview Surat" -> state.coverLetter?.let { letter ->
                val pages = remember(letter.content) { CvRenderer.letterPages(letter.content) }
                CvDocumentPreview(pages, Modifier.fillMaxWidth().height(590.dp))
                Button(onClick = { export(pages, "KarirMate-Surat-Lamaran.pdf") }, enabled = !exportBusy, modifier = Modifier.fillMaxWidth()) { Text("Ekspor Surat PDF") }
            }
        }
    }
}
