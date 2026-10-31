package com.example.automatesmartbot.ui.screens
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
    outline = Color(0xFF8995A5)
)

@Composable
fun CvBuilderScreen(viewModel: CvBuilderViewModel = viewModel()) {
    MaterialTheme(colorScheme = CvFixedColors) {

    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var page by remember { mutableStateOf(0) }
    var jobTitle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf("") }
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
    Column(Modifier.fillMaxSize().background(Color(0xFFF8FBFE))) {
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
                                        if (!pro) viewModel.setTemplate(id)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text("Catatan: desain miniatur adalah ilustrasi. Renderer ekspor khusus tiap template dan aktivasi langganan PRO belum terhubung.",
                        color = Color(0xFF66758D), fontSize = 11.sp)
                    Button(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Lanjut ke Editor") }
                }
                2 -> {
                    Text("Lengkapi Profil CV", color = navy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Pilih bagian untuk mengisi data. Setiap perubahan bisa disimpan ke CV.", color = Color(0xFF66758D), fontSize = 12.sp)
                    CvProfileSection("Informasi Pribadi", "👤", "Nama, kontak, lokasi, dan ringkasan", navy, aqua) {
                        CvBuilderBasicsEditor(state.draft) { viewModel.updateContent(it) }
                    }
                    CvProfileSection("Pendidikan", "🎓", "Sekolah, universitas, dan jurusan", navy, aqua) {
                        CvEducationSection(state.draft) { viewModel.updateContent(it) }
                    }
                    CvProfileSection("Pengalaman", "💼", "Riwayat pekerjaan dan pencapaian", navy, aqua) {
                        CvBuilderExperienceEditor(state.draft) { viewModel.updateContent(it) }
                    }
                    CvProfileSection("Keterampilan", "✦", "Keahlian dan kemampuan", navy, aqua) {
                        CvBuilderSkillsEditor(state.draft) { viewModel.updateContent(it) }
                    }
                    CvProfileSection("Objektif / Ringkasan", "◎", "Profil profesional singkat", navy, aqua) {
                        OutlinedTextField(value = state.draft.summary, onValueChange = { v -> viewModel.updateContent { it.copy(summary = v) } },
                            label = { Text("Ringkasan profil") }, placeholder = { Text("Ceritakan keahlian dan tujuan karier Anda") },
                            modifier = Modifier.fillMaxWidth(), minLines = 3,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = navy, unfocusedTextColor = navy, focusedLabelColor = navy,
                                unfocusedLabelColor = navy, focusedBorderColor = aqua, unfocusedBorderColor = Color(0xFF9EACC0)))
                    }
                    CvProfileSection("Detail Proyek", "🚀", "Portofolio dan hasil proyek", navy, aqua) {
                        CvProjectsSection(state.draft) { viewModel.updateContent(it) }
                    }
                    CvProfileSection("Sertifikasi", "🏅", "Sertifikat dan pelatihan", navy, aqua) {
                        CvCertificationsSection(state.draft) { viewModel.updateContent(it) }
                    }
                    CvProfileSection("Referensi", "♧", "Tambahkan referensi bila diperlukan", navy, aqua) {
                        Text("Referensi bersifat opsional. Fitur penyimpanan referensi akan ditambahkan pada pembaruan berikutnya.", color = navy)
                    }
                    CvProfileSection("Tambahkan Bagian Lainnya", "+", "Bahasa, organisasi, dan penghargaan", navy, aqua) {
                        Text("Bagian tambahan akan tersedia pada pembaruan berikutnya.", color = navy)
                    }
                    CvProfileSection("Ubah Urutan / Edit Judul", "↕", "Atur susunan bagian CV", navy, aqua) {
                        Text("Pengaturan urutan akan tersedia pada pembaruan berikutnya.", color = navy)
                    }
                    Button(onClick = { if (viewModel.saveDocument()) page = 3 }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Simpan & Lihat CV") }
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
        OutlinedTextField(
            value = draft.summary,
            onValueChange = { value -> onUpdate { it.copy(summary = value) } },
            label = { Text("Ringkasan profil") },
            minLines = 3,
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
            Surface(color = Color(0xFFF8FAFD), shape = RoundedCornerShape(5.dp), modifier = Modifier.fillMaxWidth().height(190.dp)) {
                Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (index % 3 == 0) Surface(color = accent, modifier = Modifier.fillMaxWidth().height(9.dp)) {}
                    Text("NAMA LENGKAP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accent)
                    Text("Posisi Profesional • Kota", fontSize = 6.sp, color = navy)
                    Surface(color = accent.copy(alpha = .85f), modifier = Modifier.fillMaxWidth().height(2.dp)) {}
                    if (index % 2 == 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = accent.copy(alpha = .17f), modifier = Modifier.weight(.38f).height(135.dp)) {
                                Column(Modifier.padding(5.dp)) {
                                    Text("KONTAK", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent)
                                    Text("Email\nTelepon\nAlamat\n\nSKILL\n• Komunikasi\n• Analisis\n• Tim", fontSize = 6.sp, lineHeight = 10.sp, color = navy)
                                }
                            }
                            Column(Modifier.weight(.62f)) {
                                Text("PROFIL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent)
                                Text("Ringkasan pengalaman dan tujuan karier.", fontSize = 6.sp, lineHeight = 9.sp, color = navy)
                                Text("PENGALAMAN", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent)
                                Text("2022–Kini\nNama Perusahaan\n• Pencapaian utama\n• Tanggung jawab\n\nPENDIDIKAN\nUniversitas", fontSize = 6.sp, lineHeight = 10.sp, color = navy)
                            }
                        }
                    } else {
                        Text("PROFIL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent)
                        Text("Ringkasan pengalaman dan tujuan karier profesional.", fontSize = 6.sp, color = navy)
                        Surface(color = accent.copy(alpha = .6f), modifier = Modifier.fillMaxWidth().height(1.dp)) {}
                        Text("PENGALAMAN KERJA", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent)
                        Text("2022–Sekarang  •  Nama Perusahaan\n• Pencapaian dan kontribusi utama\n• Mengelola proses dan tim", fontSize = 6.sp, lineHeight = 10.sp, color = navy)
                        Text("PENDIDIKAN", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent)
                        Text("Universitas / Sekolah\n2018–2022", fontSize = 6.sp, lineHeight = 10.sp, color = navy)
                        Text("KETERAMPILAN", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = accent)
                    }
                }
            }
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


// KarirMate CV Analyst V2.5 — analisis lokal berbasis CvAtsChecker, bukan AI jarak jauh.
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
    val aqua = Color(0xFF19C5CA)
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var expandedIssues by remember { mutableStateOf(true) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedFile = uri.lastPathSegment ?: "Dokumen dipilih"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sumber CV", color = navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("CV Saya • Dokumen yang sedang dibuka", color = navy, fontSize = 13.sp)
                OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                    modifier = Modifier.fillMaxWidth()) { Text("＋ Pilih PDF / DOCX dari HP", color = navy) }
                if (selectedFile != null) {
                    Text("File dipilih: $selectedFile", color = navy, fontSize = 12.sp)
                    Text("Analisis file eksternal belum aktif. Skor di bawah hanya menghitung data CV Saya, bukan file yang dipilih.",
                        color = orange, fontSize = 12.sp)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cocokkan dengan Lowongan", color = navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                OutlinedTextField(jobTitle, onJobTitleChange, label = { Text("Posisi yang dilamar") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(company, onCompanyChange, label = { Text("Nama perusahaan (opsional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(jobDescription, onJobDescriptionChange, label = { Text("Tempel deskripsi lowongan") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Button(onClick = onRun, enabled = !state.isAnalyzing, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = orange)) {
                    Text(if (state.isAnalyzing) "Sedang memeriksa..." else "Analisis CV Saya")
                }
            }
        }
        state.ats?.let { report ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Skor Kesiapan ATS", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${report.score}/100", color = if (report.score >= 75) aqua else orange,
                        fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { report.score.coerceIn(0,100) / 100f },
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        color = if (report.score >= 75) aqua else orange,
                        trackColor = Color(0xFFE9EDF3)
                    )
                    Text("Estimasi berdasarkan pemeriksaan struktur dan isi CV; bukan skor resmi ATS perusahaan.",
                        color = Color(0xFF66758D), fontSize = 12.sp)
                    Text("${report.stats.words} kata • ${report.stats.bullets} poin pengalaman • ${report.stats.quantified} pencapaian terukur",
                        color = navy, fontSize = 12.sp)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kekuatan CV (${report.passes.size})", color = navy, fontWeight = FontWeight.Bold)
                    report.passes.take(8).forEach { item -> Text("✓ ${item.title}", color = navy, fontSize = 13.sp) }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = { expandedIssues = !expandedIssues }) {
                        Text("Rekomendasi Perbaikan (${report.issues.size}) ${if (expandedIssues) "⌃" else "⌄"}", color = navy, fontWeight = FontWeight.Bold)
                    }
                    if (expandedIssues) report.issues.forEach { issue ->
                        Text("• ${issue.title}", color = orange, fontWeight = FontWeight.SemiBold)
                        Text(issue.detail, color = navy, fontSize = 12.sp)
                    }
                    if (report.issues.isEmpty()) Text("Tidak ada masalah yang terdeteksi oleh pemeriksa lokal.", color = navy)
                }
            }
        }
        state.keyword?.let { keywords ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kecocokan Kata Kunci", color = navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${(keywords.coverage * 100).toInt()}%", color = aqua, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Text("Cocok: ${keywords.matched.joinToString().ifBlank { "Belum ditemukan" }}", color = navy, fontSize = 13.sp)
                    Text("Belum ditemukan: ${keywords.missing.joinToString().ifBlank { "Tidak ada" }}", color = orange, fontSize = 13.sp)
                    if (jobDescription.isBlank()) Text("Isi deskripsi lowongan agar pencocokan kata kunci bermakna.", color = navy, fontSize = 12.sp)
                }
            }
        }
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = aqua)) { Text("Perbaiki CV di Editor", color = navy) }
        Text("Penyimpanan riwayat analisis dan analisis AI berbasis server belum tersedia pada versi ini.",
            color = Color(0xFF66758D), fontSize = 11.sp)
    }
}
