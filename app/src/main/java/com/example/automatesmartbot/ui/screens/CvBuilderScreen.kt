package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.components.*
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.ui.theme.karirButtonColors
import com.example.automatesmartbot.ui.theme.ScreenAccent as KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.ScreenPageGradient as KarirPageGradient
import com.example.automatesmartbot.ui.theme.ScreenText as KarirTextDark
import com.example.automatesmartbot.ui.theme.ScreenMuted as KarirTextMuted
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CvBuilderScreen(viewModel: CvBuilderViewModel, onBack: () -> Unit, isPro: Boolean = false) {
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
        if (uri == null) {
            snapshot?.let { java.io.File(it).delete() }
            exportSnapshot = null
        }
        if (uri != null && snapshot != null && !CvTemplates.canUse(state.templateId, isPro)) {
            java.io.File(snapshot).delete()
            exportSnapshot = null
            viewModel.message("Akses PRO tidak aktif. Ekspor CV dibatalkan.")
        } else if (uri != null && snapshot != null) scope.launch {
            exportBusy = true
            try {
                withContext(Dispatchers.IO) {
                    val type = object : com.google.gson.reflect.TypeToken<List<CvPage>>() {}.type
                    val pages: List<CvPage> = com.google.gson.Gson().fromJson(java.io.File(snapshot).readText(), type)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { CvRenderer.writePdf(context, pages, it) } ?: error("Lokasi tidak dapat ditulis.")
                }
                viewModel.message("PDF berhasil disimpan. Buka melalui aplikasi File Android.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                viewModel.message("Ekspor PDF gagal: ${e.message}")
            } finally {
                exportBusy = false
                java.io.File(snapshot).delete()
                exportSnapshot = null
            }
        }
    }
    val docxOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri ->
        if (uri != null && !CvTemplates.canUse(state.templateId, isPro)) {
            viewModel.message("Akses PRO tidak aktif. Ekspor Word dibatalkan.")
        } else if (uri != null) scope.launch {
            exportBusy = true
            try {
                withContext(Dispatchers.IO) { CvExporter.exportDocx(context, uri, state.draft) }
                viewModel.message("DOCX berhasil disimpan.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                viewModel.message("Ekspor DOCX gagal: ${e.message}")
            } finally {
                exportBusy = false
            }
        }
    }

    fun nav(destination: String) = viewModel.navigate(destination)

    fun export(pages: List<CvPage>, filename: String) {
        if (filename == "KarirMate-CV.pdf" && !CvTemplates.canUse(state.templateId, isPro)) {
            viewModel.message("Template PRO memerlukan langganan aktif terverifikasi.")
            return
        }
        try {
            val file = java.io.File.createTempFile("cv-export-", ".json", context.cacheDir)
            file.writeText(com.google.gson.Gson().toJson(pages))
            exportSnapshot = file.path
            pdfOutput.launch(filename)
        } catch (e: Exception) {
            viewModel.message("Ekspor belum dapat disiapkan: ${e.message}")
        }
    }

    if (deleteId != null) AlertDialog(
        onDismissRequest = { deleteId = null },
        title = { Text("Hapus CV?") },
        text = { Text("CV di perangkat ini akan dihapus secara permanen.") },
        confirmButton = { TextButton(onClick = { deleteId?.let(viewModel::deleteDocument); deleteId = null }) { Text("Hapus", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            KarirMateSectionHero("CV Builder", "Buat CV ATS profesional, pilih template, dan analisis skor CV Anda.")

            state.message?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = screenCanvas(Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(it, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            if (exportBusy) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = screenCanvas(Color(0xFFFFDEC9))
                )
            }

            when (page) {
                "CV Builder" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "CV Saya",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = KarirTextDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = screenCanvas(Color(0xFFFFF3E0)),
                                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9)))
                                ) {
                                    Text(
                                        "${state.drafts.size} CV Tersimpan",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                "Buat CV baru yang ramah ATS, jelajahi katalog template, atau gunakan CV Analyst.",
                                color = KarirTextMuted,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { viewModel.newDocument(); if (viewModel.state.value.activeId != null) nav("CV Saya") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = karirButtonColors()
                            ) {
                                Text("+ Buat CV Baru", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { nav("Template") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, KarirOrange),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Template", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                                OutlinedButton(
                                    onClick = { nav("CV Analyst") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, KarirOrange),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("CV Analyst", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }

                    state.drafts.forEach { document ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                            border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    document.content.contact.fullName.ifBlank { document.name },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = KarirTextDark
                                )
                                Text(
                                    "Template: ${CvTemplates.resolve(document.templateId).name}",
                                    fontSize = 12.sp,
                                    color = KarirTextMuted
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { if (viewModel.selectDocument(document.id, isPro)) nav("CV Saya") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = karirButtonColors()
                                    ) {
                                        Text("Edit CV", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { if (viewModel.selectDocument(document.id, isPro)) nav("Preview CV") },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = KarirTextDark)
                                    ) {
                                        Text("Preview", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { deleteId = document.id }) {
                                        Text("Hapus", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                "CV Saya" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Bagian-Bagian CV", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            Text("Pilih bagian untuk mengisi data. Perubahan tersimpan otomatis di perangkat.", color = KarirTextMuted, fontSize = 12.sp)

                            listOf(
                                "Informasi Pribadi", "Pendidikan", "Pengalaman", "Keterampilan",
                                "Objektif / Ringkasan", "Detail Proyek", "Sertifikasi", "Referensi",
                                "Tambahkan Bagian Lainnya", "Ubah Urutan / Edit Judul"
                            ).forEach { title ->
                                OutlinedButton(
                                    onClick = { nav(title) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = screenCanvas(Color(0xFFFFFBF7)), contentColor = KarirTextDark)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { if (!CvTemplates.canUse(state.templateId, isPro)) viewModel.message("Template PRO memerlukan langganan aktif terverifikasi.") else if (viewModel.saveDocument()) nav("Preview CV") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = karirButtonColors()
                            ) {
                                Text("Simpan & Preview CV", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                            TextButton(
                                onClick = { nav("Template") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ganti Template CV", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                "Informasi Pribadi" -> {
                    val contact = state.draft.contact
                    val bitmap by produceState<android.graphics.Bitmap?>(null, contact.photoPath) {
                        value = withContext(Dispatchers.IO) { android.graphics.BitmapFactory.decodeFile(contact.photoPath) }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Informasi Kontak & Diri", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)

                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Foto CV",
                                    modifier = Modifier.size(96.dp).clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Text("Foto bersifat opsional (format ATS standar tidak memerlukan foto).", fontSize = 11.5.sp, color = KarirTextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { photoPicker.launch(arrayOf("image/*")) },
                                    enabled = !state.photoLoading
                                ) {
                                    Text(if (contact.photoPath.isBlank()) "Pilih Foto" else "Ganti Foto", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                if (contact.photoPath.isNotBlank()) {
                                    TextButton(onClick = viewModel::removePhoto) {
                                        Text("Hapus Foto", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (state.photoLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)

                            fun update(c: CvContact) = viewModel.updateContent { it.copy(contact = c) }
                            AppInput("Nama lengkap", contact.fullName, { update(contact.copy(fullName = it)) })
                            AppInput("Posisi / profesi", contact.headline, { update(contact.copy(headline = it)) })
                            AppInput("Email", contact.email, { update(contact.copy(email = it)) })
                            AppInput("Nomor telepon", contact.phone, { update(contact.copy(phone = it)) })
                            AppInput("Domisili", contact.location, { update(contact.copy(location = it)) })
                            AppInput("Website / Portofolio", contact.website, { update(contact.copy(website = it)) })
                            AppInput("LinkedIn", contact.linkedin, { update(contact.copy(linkedin = it)) })
                            AppInput("GitHub", contact.github, { update(contact.copy(github = it)) })
                        }
                    }
                }

                "Objektif / Ringkasan" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ringkasan Profil / Objektif", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            AppInput("Tulis ringkasan singkat tentang keahlian dan pengalaman Anda", state.draft.summary, { value -> viewModel.updateContent { it.copy(summary = value) } }, minLines = 6)
                        }
                    }
                }

                "Pendidikan", "Pengalaman", "Keterampilan", "Detail Proyek", "Sertifikasi" -> {
                    CvEntriesEditor(page, state.draft, viewModel::updateContent)
                }

                "Referensi", "Tambahkan Bagian Lainnya" -> {
                    val entries = state.draft.extras.filter { if (page == "Referensi") it.id == "references" else it.id != "references" }
                    entries.forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppInput("Judul bagian", item.title, { value -> viewModel.updateContent { c -> c.copy(extras = c.extras.map { if (it.id == item.id) it.copy(title = value) else it }) } })
                                AppInput("Isi bagian", item.text, { value -> viewModel.updateContent { c -> c.copy(extras = c.extras.map { if (it.id == item.id) it.copy(text = value) else it }) } }, minLines = 4)
                                TextButton(onClick = { viewModel.updateContent { c -> c.copy(extras = c.extras.filter { it.id != item.id }) } }) {
                                    Text("Hapus bagian", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    if (page != "Referensi" || entries.isEmpty()) {
                        Button(
                            onClick = {
                                viewModel.updateContent {
                                    it.copy(extras = it.extras + CvExtraSection(if (page == "Referensi") "references" else newCvId(), if (page == "Referensi") "Referensi" else "Bahasa / Organisasi / Penghargaan"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = karirButtonColors()
                        ) {
                            Text("+ Tambah Bagian", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                "Ubah Urutan / Edit Judul" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Urutan Bagian CV", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            Text("Atur urutan susunan bagian CV Anda.", color = KarirTextMuted, fontSize = 12.sp)
                            val order = (state.draft.sectionOrder + state.draft.extras.map { it.id }).distinct()
                            order.forEachIndexed { index, id ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(id, Modifier.weight(1f), fontWeight = FontWeight.Medium, color = KarirTextDark, fontSize = 13.5.sp)
                                    TextButton(
                                        enabled = index > 0,
                                        onClick = { val next = order.toMutableList(); java.util.Collections.swap(next, index, index - 1); viewModel.updateContent { it.copy(sectionOrder = next) } }
                                    ) { Text("▲ Naik", color = MaterialTheme.colorScheme.primary) }
                                    TextButton(
                                        enabled = index < order.lastIndex,
                                        onClick = { val next = order.toMutableList(); java.util.Collections.swap(next, index, index + 1); viewModel.updateContent { it.copy(sectionOrder = next) } }
                                    ) { Text("▼ Turun", color = MaterialTheme.colorScheme.primary) }
                                }
                            }
                        }
                    }
                }

                "Template" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Katalog Template CV", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            Text("75 pilihan template CV: 27 FREE dan 48 PRO.", color = KarirTextMuted, fontSize = 12.sp)

                            val categoryList = listOf("Semua") + CvTemplates.catalog.map { it.category }.distinct()
                            ScrollableTabRow(
                                selectedTabIndex = categoryList.indexOf(filter).coerceAtLeast(0),
                                containerColor = ScreenSurface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                edgePadding = 0.dp
                            ) {
                                categoryList.forEach { categoryName ->
                                    Tab(
                                        selected = filter == categoryName,
                                        onClick = { filter = categoryName },
                                        text = {
                                            Text(
                                                categoryName,
                                                color = if (filter == categoryName) MaterialTheme.colorScheme.primary else KarirTextMuted,
                                                fontWeight = if (filter == categoryName) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    CvTemplates.catalog.filter { filter == "Semua" || it.category == filter }.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { template ->
                                Card(
                                    Modifier.weight(1f).clickable { templatePreview = template.id; nav("Preview Template") },
                                    colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        CvPageImage(remember(template.id) { CvRenderer.layout(CvTemplates.sample, template.id).first() }, 1, Modifier.fillMaxWidth(), 360)
                                        Text(template.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = KarirTextDark)
                                        Text(template.category + if (CvTemplates.isProTemplate(template.id)) " • PRO" else " • FREE", color = KarirTextMuted, fontSize = 11.sp)
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

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (empty) Text("Data contoh untuk preview. Lengkapi CV Anda sebelum ekspor.", color = MaterialTheme.colorScheme.primary, fontSize = 11.5.sp)
                            Text(CvTemplates.resolve(id).name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            val pages = remember(content, id) { CvRenderer.layout(content, id) }
                            CvDocumentPreview(pages, Modifier.fillMaxWidth().height(580.dp))

                            if (page == "Preview Template") {
                                Button(
                                    onClick = { if (CvTemplates.canUse(id, isPro)) { viewModel.setTemplate(id, isPro); nav("CV Saya") } else viewModel.message("Template ini khusus PRO. Upgrade untuk menggunakannya.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = karirButtonColors()
                                ) {
                                    Text("Gunakan Template Ini", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { export(CvRenderer.layout(state.draft, id), "KarirMate-CV.pdf") },
                                    enabled = !exportBusy && !empty && CvTemplates.canUse(id, isPro),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = karirButtonColors()
                                ) {
                                    Text("Ekspor PDF", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { if (CvTemplates.canUse(state.templateId, isPro)) docxOutput.launch("KarirMate-CV.docx") else viewModel.message("Template PRO memerlukan langganan aktif terverifikasi.") },
                                    enabled = !exportBusy && !empty,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, KarirOrange),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Ekspor Word (DOCX)", fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { nav("CV Analyst") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Analisis Skor ATS CV Ini →", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "CV Analyst" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("CV Analyst", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KarirTextDark)
                            Text("Periksa kelengkapan dan format ATS pada CV Anda.", color = KarirTextMuted, fontSize = 12.sp)

                            Button(
                                onClick = { pdfInput.launch(arrayOf("application/pdf")) },
                                enabled = !state.isAnalyzing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = karirButtonColors()
                            ) {
                                Text("Pilih PDF & Analisis", fontWeight = FontWeight.Bold)
                            }
                            if (state.isAnalyzing) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)

                            state.analysisError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                            OutlinedButton(
                                onClick = viewModel::runDraftCheck,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9)))
                            ) {
                                Text("Periksa CV Saya yang Aktif", color = KarirTextDark)
                            }

                            state.ats?.let { report ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = screenCanvas(Color(0xFFFFF3E0)),
                                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Skor Kelengkapan: ${report.score}/100", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        report.issues.forEach { Text("• ${it.title}: ${it.detail}", fontSize = 12.sp, color = KarirTextDark) }
                                    }
                                }
                            }

                            Button(
                                onClick = { useAnalyzed = false; nav("Pilih Jenis Surat") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = karirButtonColors()
                            ) {
                                Text("Buat Surat Lamaran", fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(color = screenCanvas(Color(0xFFF1E5DC)))

                            Text("Riwayat Analisis", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KarirTextDark)
                            state.analyses.forEach { a ->
                                TextButton(onClick = { viewModel.selectAnalysis(a) }) {
                                    Text("${a.source} • ${a.score}/100", color = KarirOrangeDeep)
                                }
                            }

                            Text("Surat Tersimpan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KarirTextDark)
                            state.letters.forEach { letter ->
                                TextButton(onClick = { viewModel.selectLetter(letter); jobTitle = letter.jobTitle; company = letter.company; nav("Editor Surat") }) {
                                    Text("${letter.jobTitle} — ${letter.company}", color = KarirOrangeDeep)
                                }
                            }
                        }
                    }
                }

                "Hasil Analisis" -> state.analysis?.let { report ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Hasil Analisis CV", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KarirTextDark)
                            Text("Skor kelengkapan: ${report.score}/100", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Kelebihan", fontWeight = FontWeight.Bold, color = screenCanvas(Color(0xFF166534), Color(0xFF86EFAC)))
                            report.strengths.forEach { Text("• $it", fontSize = 12.5.sp, color = KarirTextDark) }
                            Text("Kekurangan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            report.weaknesses.forEach { Text("• $it", fontSize = 12.5.sp, color = KarirTextDark) }
                            Text("Rekomendasi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            report.recommendations.forEach { Text("• $it", fontSize = 12.5.sp, color = KarirTextDark) }

                            Button(
                                onClick = { useAnalyzed = true; nav("Pilih Jenis Surat") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = karirButtonColors()
                            ) {
                                Text("Buat Surat Lamaran dari Hasil Ini", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "Pilih Jenis Surat" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Pilih Jenis Surat Lamaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            LetterType.entries.forEach { type ->
                                OutlinedButton(
                                    onClick = { letterType = type.name; nav("Tujuan Surat") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = screenCanvas(Color(0xFFFFFBF7)))
                                ) {
                                    Text(type.label, color = KarirTextDark, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                "Tujuan Surat" -> {
                    val type = LetterType.valueOf(letterType)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(type.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            if (type == LetterType.AI) {
                                Text("Integrasi generator AI memerlukan konfigurasi backend aktif.", color = KarirTextMuted, fontSize = 12.sp)
                            } else {
                                Text(
                                    if (useAnalyzed) "Sumber: Teks PDF yang telah dianalisis."
                                    else "Sumber: Data CV Saya yang aktif.",
                                    color = KarirTextMuted,
                                    fontSize = 12.sp
                                )
                                AppInput("Perusahaan tujuan", company, { company = it })
                                AppInput("Posisi tujuan", jobTitle, { jobTitle = it })
                                Button(
                                    onClick = { if (viewModel.generateCoverLetter(jobTitle, company, type, useAnalyzed)) nav("Editor Surat") },
                                    enabled = company.isNotBlank() && jobTitle.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = karirButtonColors()
                                ) {
                                    Text("Buat Draf Surat", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "Editor Surat" -> state.coverLetter?.let { letter ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Editor Surat Lamaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            Text("Surat disimpan otomatis. Anda dapat menyesuaikan isi surat sebelum ekspor.", color = KarirTextMuted, fontSize = 12.sp)
                            AppInput("Isi surat", letter.content, viewModel::updateLetter, minLines = 14)
                            Button(
                                onClick = { nav("Preview Surat") },
                                enabled = letter.content.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = karirButtonColors()
                            ) {
                                Text("Preview Surat", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "Preview Surat" -> state.coverLetter?.let { letter ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Preview Surat Lamaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                            val pages = remember(letter.content) { CvRenderer.letterPages(letter.content) }
                            CvDocumentPreview(pages, Modifier.fillMaxWidth().height(580.dp))
                            Button(
                                onClick = { export(pages, "KarirMate-Surat-Lamaran.pdf") },
                                enabled = !exportBusy,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = karirButtonColors()
                            ) {
                                Text("Ekspor Surat PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
