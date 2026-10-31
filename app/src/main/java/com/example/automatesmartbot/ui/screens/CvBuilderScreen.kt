package com.example.automatesmartbot.ui.screens
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

@Composable
fun CvBuilderScreen(viewModel: CvBuilderViewModel = viewModel()) {
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
    val pages = listOf("CV Saya", "Template", "Editor", "Preview", "CV Analyst")
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
                val selected = page == index
                Surface(
                    modifier = Modifier.clickable { page = index },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) orange else Color(0xFFEDF2F8)
                ) {
                    Text(pages[index], Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else navy)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.message?.let { Text(it, color = aqua, fontSize = 12.sp) }
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
                    Text("Pilih Template", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("30 pilihan gaya. Desain dokumen ekspor masih dalam pengembangan.", fontSize = 12.sp, color = Color.Gray)
                    val categories = listOf("ATS" to "ats_", "Modern" to "modern_", "Fresh Graduate" to "graduate_", "Executive" to "executive_", "Creative" to "creative_", "Minimalis" to "minimal_")
                    categories.forEach { (label, prefix) ->
                        Text(label, color = navy, fontWeight = FontWeight.Bold)
                        CvBuilderConstants.HTML_TEMPLATE_IDS.filter { it.startsWith(prefix) }.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                pair.forEach { id ->
                                    val chosen = id == state.templateId
                                    Card(modifier = Modifier.weight(1f).clickable { viewModel.setTemplate(id) },
                                        colors = CardDefaults.cardColors(containerColor = if (chosen) Color(0xFFE7FBFA) else Color.White),
                                        shape = RoundedCornerShape(14.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Surface(Modifier.fillMaxWidth().height(90.dp), color = Color(0xFFF3F6FA),
                                                shape = RoundedCornerShape(6.dp)) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Text("NAMA LENGKAP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = navy)
                                                    Text("━━━━━━━━━━━━", fontSize = 10.sp, color = if (id.endsWith("01")) orange else aqua)
                                                    Text("PROFIL\n━━━━━━━\nPENGALAMAN\n━━━━━━━", fontSize = 8.sp, lineHeight = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            Text(id.replace('_', ' ').uppercase(), fontSize = 11.sp, color = navy, fontWeight = FontWeight.Bold)
                                            if (chosen) Text("✓ Dipilih", color = aqua, fontSize = 11.sp)
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Button(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Lanjut Isi CV") }
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
                    Text("Analisis CV yang sedang dibuka. Impor PDF/DOCX belum tersedia pada versi ini.", color = Color.Gray, fontSize = 12.sp)
                    CvBuilderAtsPanel(state, jobTitle, company, jobDescription,
                        { jobTitle = it }, { company = it }, { jobDescription = it },
                        { viewModel.runAtsCheck(jobTitle, company, jobDescription) })
                    CvBuilderCoverLetterPanel(state, jobTitle, company) { viewModel.generateCoverLetter(jobTitle, company) }
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
