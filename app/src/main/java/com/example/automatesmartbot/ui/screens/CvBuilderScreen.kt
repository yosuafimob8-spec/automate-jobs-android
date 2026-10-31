package com.example.automatesmartbot.ui.screens
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.TextButton

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.automatesmartbot.data.cv.CvExporter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.data.cv.CvBuilderConstants
import com.example.automatesmartbot.data.cv.CvContent
import com.example.automatesmartbot.data.cv.CvExperience
import com.example.automatesmartbot.data.cv.CvSkillGroup
import com.example.automatesmartbot.data.cv.CvAtsReport
import com.example.automatesmartbot.data.cv.CvKeywordReport
import com.example.automatesmartbot.ui.theme.DarkSurfaceVariant
import com.example.automatesmartbot.ui.theme.TextWhite
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import com.example.automatesmartbot.ui.viewmodel.CvBuilderUiState


private val CvFixedColors = lightColorScheme(
    onSurface = Color(0xFF202938),
    onSurfaceVariant = Color(0xFF526071),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3F5F8),
    outline = Color(0xFF8995A5),
    primary = Color(0xFFFF622B),
    onPrimary = Color.White,
    background = Color.White
)

@Composable
fun CvBuilderScreen(viewModel: CvBuilderViewModel = viewModel()) {
    MaterialTheme(colorScheme = CvFixedColors) {
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color(0xFF202938)) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }
    var activeSection by remember { mutableStateOf<String?>(null) }
    var jobTitle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf("") }
    var templatePreviewId by remember { mutableStateOf<String?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) runCatching { CvExporter.exportPdf(context, uri, state.draft) }
            .onSuccess { exportMessage = "PDF berhasil disimpan" }
            .onFailure { exportMessage = "PDF gagal: ${it.message}" }
    }
    val docxPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri ->
        if (uri != null) runCatching { CvExporter.exportDocx(context, uri, state.draft) }
            .onSuccess { exportMessage = "DOCX berhasil disimpan" }
            .onFailure { exportMessage = "DOCX gagal: ${it.message}" }
    }
    val navy = Color(0xFF102650)
    val orange = Color(0xFFFF622B)
    val aqua = Color(0xFF19B9C3)
    val pages = listOf("CV Saya" to 0, "Template" to 1, "CV Analyst" to 4)
    Column(Modifier.fillMaxSize().background(Color(0xFFF8FBFE)).imePadding()) {
        Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
            Text("KarirMate", color = orange, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text("CV Builder", color = navy, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("Buat, kelola, dan analisis CV profesional", color = Color(0xFF657695), fontSize = 12.sp)
        }
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().background(Color.White),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(pages.size) { index ->
                val (label, destination) = pages[index]
                val selected = page == destination || (destination == 0 && page == 2) || (destination == 0 && page == 3)
                Surface(
                    modifier = Modifier.clickable { page = destination },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) orange else Color(0xFFEDF2F8)
                ) {
                    Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else navy)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.message?.let { Text(it, color = aqua, fontSize = 12.sp) }
            if (page == 2 || page == 3) {
                androidx.compose.material3.TextButton(onClick = { page = 0 }) {
                    Text("‹ Kembali ke CV Saya", color = navy, fontWeight = FontWeight.SemiBold)
                }
            }
            when (page) {
                0 -> {
                    Text("CV Saya", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { viewModel.newDocument(); page = 2 },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = orange),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buat CV Baru")
                    }
                    // CV kosong peninggalan versi lama disembunyikan, bukan dihapus.
                    val savedCvs = state.drafts.filter { doc ->
                        val c = doc.content
                        c.contact.fullName.isNotBlank() || c.contact.email.isNotBlank() ||
                        c.contact.phone.isNotBlank() || c.contact.headline.isNotBlank() ||
                        c.summary.isNotBlank() || c.experience.isNotEmpty() ||
                        c.education.isNotEmpty() || c.skills.isNotEmpty() ||
                        c.projects.isNotEmpty() || c.certifications.isNotEmpty()
                    }.distinctBy { it.id }.sortedByDescending { it.updatedAt }
                    savedCvs.forEachIndexed { index, doc ->
                        var showMenu by remember(doc.id) { mutableStateOf(false) }
                        var confirmDelete by remember(doc.id) { mutableStateOf(false) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFE6F9F9)) {
                                        Text("${index + 1}", Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            color = navy, fontWeight = FontWeight.Bold)
                                    }
                                    Box {
                                        TextButton(onClick = { showMenu = true }) { Text("⋮", color = navy, fontSize = 23.sp) }
                                        androidx.compose.material3.DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Hapus CV") },
                                                onClick = { showMenu = false; confirmDelete = true }
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    modifier = Modifier.size(70.dp),
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFE6F9F9)
                                ) {
                                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                        Text(doc.content.contact.fullName.trim().firstOrNull()?.uppercase() ?: "C",
                                            fontSize = 30.sp, fontWeight = FontWeight.Bold, color = navy)
                                    }
                                }
                                Text(doc.content.contact.fullName.ifBlank { doc.name },
                                    fontSize = 19.sp, fontWeight = FontWeight.Bold, color = navy)
                                if (doc.content.contact.email.isNotBlank()) {
                                    Text(doc.content.contact.email, fontSize = 13.sp, color = Color(0xFF657695))
                                }
                                Text(
                                    "Diperbarui: " + java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("id", "ID"))
                                        .format(java.util.Date(doc.updatedAt)),
                                    fontSize = 11.sp, color = Color(0xFF657695)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = { viewModel.selectDocument(doc.id); page = 2 },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = orange),
                                        shape = RoundedCornerShape(20.dp)) { Text("Edit") }
                                    Button(onClick = { viewModel.selectDocument(doc.id); page = 3 },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = aqua),
                                        shape = RoundedCornerShape(20.dp)) { Text("Lihat CV") }
                                }
                            }
                        }
                        if (confirmDelete) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                title = { Text("Hapus CV?") },
                                text = { Text("CV ini akan dihapus permanen dari perangkat.") },
                                confirmButton = {
                                    TextButton(onClick = { viewModel.deleteDocument(doc.id); confirmDelete = false }) {
                                        Text("Hapus", color = orange)
                                    }
                                },
                                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } }
                            )
                        }
                    }
                }
                1 -> {
                    var selectedFilter by remember { mutableStateOf("Semua") }
                    val filters = listOf("Semua", "ATS", "Modern", "Profesional", "Fresh Graduate", "Eksekutif", "Kreatif", "Minimalis", "Gratis", "PRO")
                    Text("Pilih Template CV", color = navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Pilih desain sesuai kebutuhan. Template PRO ditandai dengan ikon mahkota.", color = Color(0xFF66758D), fontSize = 12.sp)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(filters.size) { index ->
                            val label = filters[index]
                            Surface(modifier = Modifier.clickable { selectedFilter = label }, shape = RoundedCornerShape(22.dp),
                                color = if (selectedFilter == label) orange else Color(0xFFE9EFF7)) {
                                Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = if (selectedFilter == label) Color.White else navy, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    val allTemplates = CvBuilderConstants.HTML_TEMPLATE_IDS
                    val visibleTemplates = allTemplates.filter { id ->
                        val group = id.substringBefore('_')
                        val pro = CvTemplateIsPro(id)
                        when (selectedFilter) {
                            "Semua" -> true
                            "Gratis" -> !pro
                            "PRO" -> pro
                            "Profesional" -> group == "ats" || group == "executive"
                            "ATS" -> group == "ats"
                            "Modern" -> group == "modern"
                            "Fresh Graduate" -> group == "graduate"
                            "Eksekutif" -> group == "executive"
                            "Kreatif" -> group == "creative"
                            "Minimalis" -> group == "minimal"
                            else -> true
                        }
                    }
                    Text("${visibleTemplates.size} desain tersedia", color = navy, fontWeight = FontWeight.SemiBold)
                    visibleTemplates.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { id ->
                                val pro = CvTemplateIsPro(id)
                                val selected = state.templateId == id
                                Column(Modifier.weight(1f)) {
                                    CvTemplateCard(id, pro, selected, navy, orange, aqua) {
                                        templatePreviewId = id
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    templatePreviewId?.let { previewId ->
                        val locked = CvTemplateIsPro(previewId)
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { templatePreviewId = null },
                            title = { Text("Preview • " + previewId.replace('_', ' ').uppercase(), color = navy) },
                            text = {
                                Column(Modifier.fillMaxWidth().height(440.dp).verticalScroll(rememberScrollState())) {
                                    CvTemplateArtwork(previewId, navy, aqua, Modifier.fillMaxWidth().height(310.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(if (locked) "Template PRO • preview gratis, penggunaan memerlukan langganan aktif." else "Template gratis • siap dipilih.", color = navy)
                                    Text("Preview merupakan contoh visual. Ekspor PDF khusus template masih dalam pengembangan.", fontSize = 11.sp, color = Color(0xFF66758D))
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (!locked) { viewModel.setTemplate(previewId); templatePreviewId = null; page = 2 }
                                }, enabled = !locked, colors = ButtonDefaults.buttonColors(containerColor = orange)) {
                                    Text(if (locked) "Perlu PRO" else "Gunakan Template")
                                }
                            },
                            dismissButton = { TextButton(onClick = { templatePreviewId = null }) { Text("Tutup") } }
                        )
                    }
                    Text("Catatan: desain miniatur adalah ilustrasi. Renderer ekspor khusus tiap template dan aktivasi langganan PRO belum terhubung.",
                        color = Color(0xFF66758D), fontSize = 11.sp)
                    Button(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Lanjut ke Editor") }
                }
                2 -> {
                    if (activeSection == null) {
                        Text("Lengkapi Profil CV", color = navy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Pilih bagian CV untuk membuka halaman pengisian.", color = Color(0xFF526071))
                        listOf(
                            Triple("Informasi Pribadi", "👤", "Nama, kontak, dan domisili"),
                            Triple("Pendidikan", "🎓", "Sekolah, universitas, dan jurusan"),
                            Triple("Pengalaman", "💼", "Riwayat pekerjaan dan pencapaian"),
                            Triple("Keterampilan", "✦", "Keahlian dan kemampuan"),
                            Triple("Objektif / Ringkasan", "◎", "Profil profesional singkat"),
                            Triple("Detail Proyek", "🚀", "Portofolio dan hasil proyek"),
                            Triple("Sertifikasi", "🏅", "Sertifikat dan pelatihan"),
                            Triple("Referensi", "♧", "Referensi profesional"),
                            Triple("Tambahkan Bagian Lainnya", "+", "Bahasa, organisasi, dan penghargaan"),
                            Triple("Ubah Urutan / Edit Judul", "↕", "Susunan bagian CV")
                        ).forEach { (title, symbol, subtitle) ->
                            Card(modifier = Modifier.fillMaxWidth().clickable { activeSection = title },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                                Row(Modifier.fillMaxWidth().padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(color = Color(0xFFE7F9FA), shape = RoundedCornerShape(12.dp)) {
                                        Text(symbol, Modifier.padding(12.dp), fontSize = 22.sp, color = navy)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(title, color = navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(subtitle, color = Color(0xFF526071), fontSize = 12.sp)
                                    }
                                    Text("›", color = aqua, fontSize = 26.sp)
                                }
                            }
                        }
                        Button(onClick = { if (viewModel.saveDocument()) page = 3 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = orange)) {
                            Text("Simpan & Lihat CV", color = Color.White)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { activeSection = null }) { Text("‹ Kembali", color = navy) }
                            Text(activeSection.orEmpty(), color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (activeSection) {
                                    "Informasi Pribadi" -> CvBuilderBasicsEditor(state.draft) { viewModel.updateContent(it) }
                                    "Pendidikan" -> CvEducationSection(state.draft) { viewModel.updateContent(it) }
                                    "Pengalaman" -> CvBuilderExperienceEditor(state.draft) { viewModel.updateContent(it) }
                                    "Keterampilan" -> CvBuilderSkillsEditor(state.draft) { viewModel.updateContent(it) }
                                    "Objektif / Ringkasan" -> OutlinedTextField(
                                        value = state.draft.summary,
                                        onValueChange = { value -> viewModel.updateContent { it.copy(summary = value) } },
                                        label = { Text("Ringkasan profil") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                                    "Detail Proyek" -> CvProjectsSection(state.draft) { viewModel.updateContent(it) }
                                    "Sertifikasi" -> CvCertificationsSection(state.draft) { viewModel.updateContent(it) }
                                    "Referensi" -> Text("Penyimpanan referensi belum didukung oleh model data CV.", color = navy)
                                    "Tambahkan Bagian Lainnya" -> Text("Bagian tambahan memerlukan dukungan model data CV.", color = navy)
                                    "Ubah Urutan / Edit Judul" -> Text("Pengaturan urutan memerlukan dukungan renderer CV.", color = navy)
                                }
                            }
                        }
                        Button(onClick = { if (viewModel.saveDocument()) activeSection = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = orange)) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan", color = Color.White)
                        }
                    }
                }
                3 -> {
                    Text("Preview CV", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    CvBuilderPreview(state.draft, state.templateId)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { pdfPicker.launch("KarirMate-CV.pdf") }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("PDF") }
                        Button(onClick = { docxPicker.launch("KarirMate-CV.docx") }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = aqua)) { Text("DOCX") }
                    }
                    if (exportMessage.isNotBlank()) Text(exportMessage, color = navy)
                    OutlinedButton(onClick = { page = 4 }, modifier = Modifier.fillMaxWidth()) { Text("Analisis CV") }
                }
                else -> {
                    Text("CV Analyst", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Periksa kesiapan CV dan kecocokan dengan lowongan.", color = Color.Gray, fontSize = 12.sp)
                    KarirMateAnalystV25(
                        state = state,
                        jobTitle = jobTitle,
                        company = company,
                        jobDescription = jobDescription,
                        onJobTitleChange = { jobTitle = it },
                        onCompanyChange = { company = it },
                        onJobDescriptionChange = { jobDescription = it },
                        onRun = { viewModel.runAtsCheck(jobTitle, company, jobDescription) },
                        onEdit = { page = 2 }
                    )
                    CvBuilderCoverLetterPanel(state, jobTitle, company) { viewModel.generateCoverLetter(jobTitle, company) }
                }
            }
        }
    }

    }
    }
}

@Composable
private fun CvExtraEditor(draft: CvContent, onUpdate: ((CvContent) -> CvContent) -> Unit) {
    Text("Pendidikan", fontWeight = FontWeight.Bold)
    draft.education.forEachIndexed { i, item ->
        OutlinedTextField(item.school, { value -> onUpdate { c -> c.copy(education = c.education.mapIndexed { j, e -> if (i == j) e.copy(school = value) else e }) } }, label = { Text("Sekolah / Universitas") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(item.degree, { value -> onUpdate { c -> c.copy(education = c.education.mapIndexed { j, e -> if (i == j) e.copy(degree = value) else e }) } }, label = { Text("Jenjang pendidikan") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { onUpdate { it.copy(education = it.education.filterIndexed { j, _ -> i != j }) } }) { Text("Hapus pendidikan") }
    }
    OutlinedButton(onClick = { onUpdate { it.copy(education = it.education + com.example.automatesmartbot.data.cv.CvEducation(id = System.nanoTime().toString())) } }) { Text("+ Pendidikan") }
    Text("Sertifikasi", fontWeight = FontWeight.Bold)
    draft.certifications.forEachIndexed { i, item ->
        OutlinedTextField(item.name, { value -> onUpdate { c -> c.copy(certifications = c.certifications.mapIndexed { j, e -> if (i == j) e.copy(name = value) else e }) } }, label = { Text("Nama sertifikat") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(item.issuer, { value -> onUpdate { c -> c.copy(certifications = c.certifications.mapIndexed { j, e -> if (i == j) e.copy(issuer = value) else e }) } }, label = { Text("Penerbit") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { onUpdate { it.copy(certifications = it.certifications.filterIndexed { j, _ -> i != j }) } }) { Text("Hapus sertifikat") }
    }
    OutlinedButton(onClick = { onUpdate { it.copy(certifications = it.certifications + com.example.automatesmartbot.data.cv.CvCertification(id = System.nanoTime().toString())) } }) { Text("+ Sertifikasi") }
    Text("Proyek", fontWeight = FontWeight.Bold)
    draft.projects.forEachIndexed { i, item ->
        OutlinedTextField(item.name, { value -> onUpdate { c -> c.copy(projects = c.projects.mapIndexed { j, e -> if (i == j) e.copy(name = value) else e }) } }, label = { Text("Nama proyek") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(item.tech, { value -> onUpdate { c -> c.copy(projects = c.projects.mapIndexed { j, e -> if (i == j) e.copy(tech = value) else e }) } }, label = { Text("Teknologi / keterampilan") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { onUpdate { it.copy(projects = it.projects.filterIndexed { j, _ -> i != j }) } }) { Text("Hapus proyek") }
    }
    OutlinedButton(onClick = { onUpdate { it.copy(projects = it.projects + com.example.automatesmartbot.data.cv.CvProject(id = System.nanoTime().toString())) } }) { Text("+ Proyek") }
}

@Composable
private fun CvBuilderPreview(draft: CvContent, templateId: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("Preview CV • $templateId", fontWeight = FontWeight.Bold)
            Text(draft.contact.fullName.ifBlank { "Nama Lengkap" }, fontSize = 21.sp)
            Text(draft.contact.headline)
            Text(listOf(draft.contact.email, draft.contact.phone, draft.contact.location)
                .filter { it.isNotBlank() }.joinToString(" • "))
            if (draft.summary.isNotBlank()) {
                Text("PROFIL", fontWeight = FontWeight.Bold)
                Text(draft.summary)
            }
            draft.experience.forEach {
                Text(it.title + " — " + it.company, fontWeight = FontWeight.Medium)
                Text(it.bullets.joinToString("\\n"))
            }
            draft.skills.forEach {
                Text(it.category + ": " + it.items.joinToString(", "))
            }
        }
    }
}

@Composable
private fun CvBuilderBasicsEditor(
    draft: CvContent,
    onUpdate: ((CvContent) -> CvContent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Data Pribadi", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = draft.contact.fullName,
            onValueChange = { value -> onUpdate { it.copy(contact = it.contact.copy(fullName = value)) } },
            label = { Text("Nama lengkap") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.contact.headline,
            onValueChange = { value -> onUpdate { it.copy(contact = it.contact.copy(headline = value)) } },
            label = { Text("Posisi / profesi") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.contact.email,
            onValueChange = { value -> onUpdate { it.copy(contact = it.contact.copy(email = value)) } },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.contact.phone,
            onValueChange = { value -> onUpdate { it.copy(contact = it.contact.copy(phone = value)) } },
            label = { Text("Nomor telepon") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.contact.location,
            onValueChange = { value -> onUpdate { it.copy(contact = it.contact.copy(location = value)) } },
            label = { Text("Domisili") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CvBuilderExperienceEditor(
    draft: CvContent,
    onUpdate: ((CvContent) -> CvContent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pengalaman Kerja", fontWeight = FontWeight.Bold)
        draft.experience.forEachIndexed { index, experience ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = experience.title,
                        onValueChange = { value ->
                            onUpdate { content ->
                                content.copy(experience = content.experience.mapIndexed { i, e ->
                                    if (i == index) e.copy(title = value) else e
                                })
                            }
                        },
                        label = { Text("Jabatan") }
                    )
                    OutlinedTextField(
                        value = experience.company,
                        onValueChange = { value ->
                            onUpdate { content ->
                                content.copy(experience = content.experience.mapIndexed { i, e ->
                                    if (i == index) e.copy(company = value) else e
                                })
                            }
                        },
                        label = { Text("Perusahaan") }
                    )
                    OutlinedTextField(
                        value = experience.bullets.joinToString("\\n"),
                        onValueChange = { value ->
                            onUpdate { content ->
                                content.copy(experience = content.experience.mapIndexed { i, e ->
                                    if (i == index) e.copy(bullets = value.lines()) else e
                                })
                            }
                        },
                        label = { Text("Deskripsi pekerjaan") },
                        minLines = 2
                    )
                    OutlinedButton(onClick = {
                        onUpdate { it.copy(experience = it.experience.filterIndexed { i, _ -> i != index }) }
                    }) {
                        Text("Hapus pengalaman")
                    }
                }
            }
        }
        Button(onClick = {
            onUpdate {
                it.copy(experience = it.experience + CvExperience(id = System.nanoTime().toString()))
            }
        }) {
            Text("+ Tambah Pengalaman")
        }
    }
}

@Composable
private fun CvBuilderSkillsEditor(
    draft: CvContent,
    onUpdate: ((CvContent) -> CvContent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Keahlian", fontWeight = FontWeight.Bold)
        draft.skills.forEachIndexed { index, group ->
            OutlinedTextField(
                value = group.category,
                onValueChange = { value ->
                    onUpdate { content ->
                        content.copy(skills = content.skills.mapIndexed { i, g ->
                            if (i == index) g.copy(category = value) else g
                        })
                    }
                },
                label = { Text("Kategori keahlian") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = group.items.joinToString(", "),
                onValueChange = { value ->
                    onUpdate { content ->
                        content.copy(skills = content.skills.mapIndexed { i, g ->
                            if (i == index) g.copy(items = value.split(",").map { it.trim() }) else g
                        })
                    }
                },
                label = { Text("Keahlian, pisahkan dengan koma") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = {
                onUpdate { it.copy(skills = it.skills.filterIndexed { i, _ -> i != index }) }
            }) {
                Text("Hapus kelompok")
            }
        }
        Button(onClick = {
            onUpdate {
                it.copy(skills = it.skills + CvSkillGroup(id = System.nanoTime().toString()))
            }
        }) {
            Text("+ Tambah Keahlian")
        }
    }
}

@Composable
private fun CvBuilderAtsPanel(
    state: CvBuilderUiState,
    jobTitle: String,
    company: String,
    jobDescription: String,
    onJobTitleChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onJobDescriptionChange: (String) -> Unit,
    onRunAnalysis: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Analisis ATS", fontWeight = FontWeight.Bold)
        OutlinedTextField(jobTitle, onJobTitleChange, label = { Text("Posisi") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(company, onCompanyChange, label = { Text("Perusahaan") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(jobDescription, onJobDescriptionChange,
            label = { Text("Deskripsi lowongan") }, minLines = 3,
            modifier = Modifier.fillMaxWidth())
        Button(onClick = onRunAnalysis, enabled = !state.isAnalyzing) {
            Text(if (state.isAnalyzing) "Menganalisis..." else "Periksa ATS")
        }
        state.ats?.let { report ->
            Text("Skor ATS: ${report.score}/100", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (report.ready) "Siap diperiksa" else "Perlu perbaikan")
            report.issues.forEach { issue -> Text("• ${issue.title}: ${issue.detail}", fontSize = 12.sp) }
        }
        state.keyword?.let { report ->
            Text("Kecocokan kata kunci: ${(report.coverage * 100).toInt()}%")
            Text("Cocok: ${report.matched.joinToString(", ")}")
            Text("Belum cocok: ${report.missing.joinToString(", ")}")
        }
    }
}

@Composable
private fun CvBuilderCoverLetterPanel(
    state: CvBuilderUiState,
    jobTitle: String,
    company: String,
    onGenerate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Surat Lamaran", fontWeight = FontWeight.Bold)
        Button(onClick = onGenerate) {
            Text("Buat Surat Lamaran")
        }
        state.coverLetter?.let { Text(it.content) }
    }
}


@Composable
private fun CvProfileSection(title: String, symbol: String, subtitle: String, navy: Color, aqua: Color, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = Color(0xFFE7F9FA), shape = RoundedCornerShape(12.dp)) {
                    Text(symbol, modifier = Modifier.padding(12.dp), fontSize = 22.sp, color = navy)
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(subtitle, color = Color(0xFF687991), fontSize = 11.sp)
                }
                Text(if (expanded) "⌃" else "⌄", color = aqua, fontSize = 24.sp)
            }
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
        }
    }
}

@Composable
private fun CvEducationSection(draft: CvContent, onUpdate: ((CvContent) -> CvContent) -> Unit) {
    draft.education.forEachIndexed { i, item ->
        OutlinedTextField(item.school, { value -> onUpdate { c -> c.copy(education = c.education.mapIndexed { j, e -> if (i == j) e.copy(school = value) else e }) } }, label = { Text("Sekolah / Universitas") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(item.degree, { value -> onUpdate { c -> c.copy(education = c.education.mapIndexed { j, e -> if (i == j) e.copy(degree = value) else e }) } }, label = { Text("Jenjang pendidikan") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { onUpdate { it.copy(education = it.education.filterIndexed { j, _ -> i != j }) } }) { Text("Hapus pendidikan") }
    }
    OutlinedButton(onClick = { onUpdate { it.copy(education = it.education + com.example.automatesmartbot.data.cv.CvEducation(id = System.nanoTime().toString())) } }) { Text("+ Tambah Pendidikan") }
}

@Composable
private fun CvProjectsSection(draft: CvContent, onUpdate: ((CvContent) -> CvContent) -> Unit) {
    draft.projects.forEachIndexed { i, item ->
        OutlinedTextField(item.name, { value -> onUpdate { c -> c.copy(projects = c.projects.mapIndexed { j, e -> if (i == j) e.copy(name = value) else e }) } }, label = { Text("Nama proyek") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(item.tech, { value -> onUpdate { c -> c.copy(projects = c.projects.mapIndexed { j, e -> if (i == j) e.copy(tech = value) else e }) } }, label = { Text("Teknologi / keterampilan") }, modifier = Modifier.fillMaxWidth())
    }
    OutlinedButton(onClick = { onUpdate { it.copy(projects = it.projects + com.example.automatesmartbot.data.cv.CvProject(id = System.nanoTime().toString())) } }) { Text("+ Tambah Proyek") }
}

@Composable
private fun CvCertificationsSection(draft: CvContent, onUpdate: ((CvContent) -> CvContent) -> Unit) {
    draft.certifications.forEachIndexed { i, item ->
        OutlinedTextField(item.name, { value -> onUpdate { c -> c.copy(certifications = c.certifications.mapIndexed { j, e -> if (i == j) e.copy(name = value) else e }) } }, label = { Text("Nama sertifikat") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(item.issuer, { value -> onUpdate { c -> c.copy(certifications = c.certifications.mapIndexed { j, e -> if (i == j) e.copy(issuer = value) else e }) } }, label = { Text("Penerbit") }, modifier = Modifier.fillMaxWidth())
    }
    OutlinedButton(onClick = { onUpdate { it.copy(certifications = it.certifications + com.example.automatesmartbot.data.cv.CvCertification(id = System.nanoTime().toString())) } }) { Text("+ Tambah Sertifikasi") }
}


@Composable
private fun CvTemplateArtwork(id: String, navy: Color, aqua: Color, modifier: Modifier = Modifier) {
    val group = id.substringBefore('_')
    val n = id.substringAfterLast('_').toIntOrNull() ?: 1
    val accent = when (group) {
        "ats" -> Color(0xFF273750)
        "modern" -> aqua
        "graduate" -> Color(0xFF3D80B9)
        "executive" -> Color(0xFF27325A)
        "creative" -> Color(0xFFEE7045)
        else -> Color(0xFF5C6472)
    }
    val dark = group == "executive" && n % 3 == 0 || group == "creative" && n % 4 == 0
    val ink = if (dark) Color.White else navy
    val bg = if (dark) Color(0xFF202B43) else Color(0xFFF8FAFD)
    Surface(color = bg, shape = RoundedCornerShape(5.dp), modifier = modifier) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (n % 4 == 3 || group == "creative") Surface(color = accent, modifier = Modifier.fillMaxWidth().height(if (group == "creative") 13.dp else 6.dp)) {}
            if (n % 5 == 0) Text("CV  /  PORTFOLIO", color = accent, fontSize = 7.sp, letterSpacing = 2.sp)
            if (group == "executive" || group == "minimal") {
                Text("NAMA LENGKAP", fontSize = if (group == "executive") 17.sp else 12.sp, color = ink, fontWeight = FontWeight.Bold)
                Text("PROFESSIONAL PROFILE", fontSize = 7.sp, color = accent)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (group == "creative" || n % 4 == 2) Surface(color = accent.copy(alpha = .25f), shape = RoundedCornerShape(30.dp), modifier = Modifier.size(26.dp)) {}
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text("NAMA LENGKAP", fontSize = 11.sp, color = ink, fontWeight = FontWeight.Bold)
                        Text("Posisi Profesional • Kota", fontSize = 6.sp, color = ink)
                    }
                }
            }
            if (group == "ats" || group == "minimal" || n % 4 == 1) {
                Surface(color = accent.copy(alpha = .7f), modifier = Modifier.fillMaxWidth().height(1.dp)) {}
                Text("PROFIL", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("Ringkasan pengalaman profesional dan tujuan karier.", color = ink, fontSize = 7.sp)
                Text("PENGALAMAN KERJA", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("2022–Sekarang  |  Nama Perusahaan\n• Pencapaian dan tanggung jawab utama\n• Mengelola proses dan tim", color = ink, fontSize = 7.sp, lineHeight = 11.sp)
                Text("PENDIDIKAN", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text("Universitas / Sekolah • 2018–2022", color = ink, fontSize = 7.sp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    val left = if (n % 3 == 0) .60f else .36f
                    Column(Modifier.weight(left).background(accent.copy(alpha = if (dark) .28f else .15f)).padding(5.dp)) {
                        Text("KONTAK", fontSize = 8.sp, color = ink, fontWeight = FontWeight.Bold)
                        Text("Email\nTelepon\nLokasi\n\nKEAHLIAN\n• Analisis\n• Komunikasi\n• Tim", fontSize = 7.sp, lineHeight = 12.sp, color = ink)
                    }
                    Column(Modifier.weight(1f-left)) {
                        Text("PROFIL", fontSize = 8.sp, color = accent, fontWeight = FontWeight.Bold)
                        Text("Ringkasan pengalaman profesional.", fontSize = 7.sp, color = ink)
                        Text("PENGALAMAN", fontSize = 8.sp, color = accent, fontWeight = FontWeight.Bold)
                        Text("2022–Kini\nNama Perusahaan\n• Pencapaian utama\n• Tanggung jawab\n\nPENDIDIKAN\nUniversitas", fontSize = 7.sp, lineHeight = 12.sp, color = ink)
                    }
                }
            }
        }
    }
}

// Gallery preview only. The existing exporter is intentionally untouched.
private fun CvTemplateIsPro(id: String): Boolean =
    id.substringAfterLast('_').toIntOrNull()?.let { it >= 4 } ?: false

@Composable
private fun CvTemplateCard(id: String, pro: Boolean, selected: Boolean, navy: Color, orange: Color, aqua: Color, onChoose: () -> Unit) {
    val index = id.substringAfterLast('_').toIntOrNull() ?: 1
    val group = id.substringBefore('_')
    val accent = when (group) {
        "ats" -> navy
        "modern" -> aqua
        "graduate" -> Color(0xFF4D82C8)
        "executive" -> Color(0xFF293A6B)
        "creative" -> orange
        else -> Color(0xFF5D6C83)
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onChoose), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) aqua else Color(0xFFE0E6EF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CvTemplateArtwork(id, navy, aqua, Modifier.fillMaxWidth().height(190.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text(id.replace('_', ' ').uppercase(), fontSize = 9.sp, color = navy, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Surface(color = if (pro) Color(0xFFFFE3AA) else Color(0xFFE1F8F6), shape = RoundedCornerShape(8.dp)) {
                    Text(if (pro) "♛ PRO" else "FREE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        color = if (pro) Color(0xFF8A5200) else Color(0xFF087A81), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(if (pro) "🔒 Perlu PRO (belum aktif)" else if (selected) "✓ Template dipilih" else "Ketuk untuk memilih",
                color = if (selected) aqua else Color(0xFF66758D), fontSize = 10.sp)
        }
    }
}


// CV Analyze V2.6: separate dashboard, upload, result, and history screens.
// The existing local checker analyzes the current CV draft, not external PDFs.
@Composable
private fun KarirMateAnalystV25(
    state: CvBuilderUiState,
    jobTitle: String,
    company: String,
    jobDescription: String,
    onJobTitleChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onJobDescriptionChange: (String) -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit
) {
    val navy = Color(0xFF102650)
    val orange = Color(0xFFFF622B)
    val blue = Color(0xFF2468D9)
    val muted = Color(0xFF64748B)
    var screen by remember { mutableStateOf("home") }
    var chosenFile by remember { mutableStateOf<String?>(null) }
    var showAll by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) chosenFile = uri.lastPathSegment ?: "PDF dipilih"
    }
    val report = state.ats
    val keywords = state.keyword
    @Composable
    fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (screen != "home") {
            TextButton(onClick = { screen = "home" }) { Text("← Kembali ke CV Analyze", color = navy) }
        }
        when (screen) {
            "home" -> {
                Text("CV ATS Analyzer", color = navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                SectionCard {
                    Text("SKOR ATS TERAKHIR", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (report != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            androidx.compose.material3.CircularProgressIndicator(
                                progress = { report.score.coerceIn(0, 100) / 100f },
                                modifier = Modifier.size(84.dp), color = blue, strokeWidth = 9.dp
                            )
                            Column {
                                Text("${report.score}/100", color = navy, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                Text("Skor kesiapan CV Saya", color = muted, fontSize = 12.sp)
                            }
                        }
                        Button(onClick = { screen = "result" }, colors = ButtonDefaults.buttonColors(containerColor = blue),
                            modifier = Modifier.fillMaxWidth()) { Text("Lihat Hasil Analisis") }
                    } else {
                        Text("Belum ada analisis CV", color = navy)
                        Text("Analisis CV Saya untuk melihat skor dan rekomendasi.", color = muted, fontSize = 12.sp)
                    }
                }
                SectionCard {
                    Text("Analisis CV Baru", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Periksa struktur CV dan kecocokan kata kunci lowongan.", color = muted, fontSize = 13.sp)
                    Button(onClick = { screen = "upload" }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Mulai Analisis →") }
                }
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Riwayat Terbaru", color = navy, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { screen = "history" }) { Text("Lihat Semua", color = blue) }
                    }
                    if (report == null) Text("Belum ada hasil analisis tersimpan.", color = muted)
                    else Text("CV Saya  •  Skor ${report.score}/100", color = navy)
                }
            }
            "upload" -> {
                Text("Analisis CV", color = navy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                SectionCard {
                    Text("Upload CV", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth().height(94.dp)) {
                        Text("↑  Pilih PDF dari HP", color = blue, fontWeight = FontWeight.Bold)
                    }
                    if (chosenFile != null) {
                        Text("Dipilih: $chosenFile", color = navy, fontSize = 12.sp)
                        Text("Analisis isi PDF eksternal belum tersedia. Untuk sekarang, skor hanya berasal dari data CV Saya.",
                            color = orange, fontSize = 12.sp)
                    }
                    Text("Cocokkan dengan Lowongan (opsional)", color = navy, fontWeight = FontWeight.Bold)
                    OutlinedTextField(jobTitle, onJobTitleChange, label = { Text("Posisi pekerjaan") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(company, onCompanyChange, label = { Text("Perusahaan") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(jobDescription, onJobDescriptionChange, label = { Text("Deskripsi lowongan") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Button(onClick = { onRun(); screen = "result" }, enabled = !state.isAnalyzing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = blue)) {
                        Text(if (state.isAnalyzing) "Menganalisis..." else "Analisis CV Saya Sekarang")
                    }
                }
                Text("PARAMETER PEMERIKSAAN", color = muted, fontWeight = FontWeight.Bold)
                listOf("Skor ATS" to "Struktur dan kelengkapan CV",
                    "Keyword Match" to "Kecocokan dengan deskripsi lowongan",
                    "Formatting" to "Keterbacaan dan susunan informasi").forEach { (title, detail) ->
                    SectionCard { Text(title, color = navy, fontWeight = FontWeight.Bold); Text(detail, color = muted) }
                }
            }
            "result" -> {
                Text("Hasil Analisis", color = navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (report == null) {
                    SectionCard {
                        Text(if (state.isAnalyzing) "Sedang menganalisis CV..." else "Belum ada hasil analisis.", color = navy)
                        if (!state.isAnalyzing) OutlinedButton(onClick = { screen = "upload" }) { Text("Mulai Analisis") }
                    }
                } else {
                    SectionCard {
                        Text("SKOR KESIAPAN ATS", color = muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${report.score}/100", color = blue, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { report.score.coerceIn(0,100) / 100f },
                            modifier = Modifier.fillMaxWidth().height(9.dp), color = blue,
                            trackColor = Color(0xFFE9EDF3))
                        Text("Estimasi pemeriksaan lokal, bukan skor resmi ATS perusahaan.", color = muted, fontSize = 12.sp)
                        Text("${report.stats.words} kata • ${report.stats.bullets} poin • ${report.stats.quantified} pencapaian terukur",
                            color = navy, fontSize = 12.sp)
                    }
                    SectionCard {
                        Text("Skills & Keywords", color = navy, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        if (keywords == null || jobDescription.isBlank()) {
                            Text("Masukkan deskripsi lowongan untuk membandingkan kata kunci.", color = muted)
                        } else {
                            Text("Kecocokan kata kunci: ${(keywords.coverage * 100).toInt()}%", color = blue, fontWeight = FontWeight.Bold)
                            Text("✓ Kata kunci cocok", color = Color(0xFF16805B), fontWeight = FontWeight.Bold)
                            Text(keywords.matched.joinToString(" • ").ifBlank { "Belum ada" }, color = navy)
                            Text("⚠ Kata kunci belum ditemukan", color = orange, fontWeight = FontWeight.Bold)
                            Text(keywords.missing.joinToString(" • ").ifBlank { "Tidak ada" }, color = navy)
                        }
                    }
                    SectionCard {
                        Text("Kekuatan CV", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        report.passes.forEach { Text("✓ ${it.title}", color = navy) }
                        if (report.passes.isEmpty()) Text("Belum ada poin yang terdeteksi.", color = muted)
                    }
                    SectionCard {
                        Text("Rekomendasi Perbaikan", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        report.issues.forEach {
                            Text("• ${it.title}", color = orange, fontWeight = FontWeight.Bold)
                            Text(it.detail, color = navy, fontSize = 13.sp)
                        }
                        if (report.issues.isEmpty()) Text("Tidak ada masalah yang terdeteksi.", color = muted)
                    }
                    Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Perbaiki CV di Editor") }
                }
            }
            "history" -> {
                Text("Riwayat Analisis", color = navy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                SectionCard {
                    if (report == null) Text("Belum ada riwayat.", color = muted)
                    else {
                        Text("Hasil terakhir • CV Saya", color = navy, fontWeight = FontWeight.Bold)
                        Text("Skor ATS: ${report.score}/100", color = blue)
                        OutlinedButton(onClick = { screen = "result" }) { Text("Lihat Hasil") }
                    }
                    Text("Riwayat analisis permanen belum tersedia. Hasil terakhir ditampilkan selama sesi ini.",
                        color = muted, fontSize = 12.sp)
                }
            }
        }
    }
}
