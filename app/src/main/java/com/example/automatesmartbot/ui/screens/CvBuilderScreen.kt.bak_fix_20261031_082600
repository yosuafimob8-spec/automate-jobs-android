package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.background
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
fun CvBuilderScreen(
    viewModel: CvBuilderViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var exportError by remember { mutableStateOf<String?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) runCatching { CvExporter.exportPdf(context, uri, state.draft) }
            .onFailure { exportError = "Ekspor PDF gagal: ${it.message}" }
    }
    val docxPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { uri ->
        if (uri != null) runCatching { CvExporter.exportDocx(context, uri, state.draft) }
            .onFailure { exportError = "Ekspor DOCX gagal: ${it.message}" }
    }
    val draft = state.draft
    var jobTitle by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CV Builder",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Buat, edit, dan ekspor CV dari data Anda sendiri.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.newDocument() }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF1267E8),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Medium
            )
        }

        CvBuilderTemplateSelector(
            templateId = state.templateId,
            latexStyle = state.latexStyle,
            onTemplateChange = { viewModel.setTemplate(it) },
            onLatexStyleChange = { viewModel.setLatexStyle(it) }
        )

        CvBuilderPreview(
            draft = draft,
            templateId = state.templateId
        )

        CvBuilderBasicsEditor(
            draft = draft,
            onUpdate = { transform -> viewModel.updateContent(transform) }
        )

        CvBuilderExperienceEditor(
            draft = draft,
            onUpdate = { transform -> viewModel.updateContent(transform) }
        )

        CvBuilderSkillsEditor(
            draft = draft,
            onUpdate = { transform -> viewModel.updateContent(transform) }
        )

        CvBuilderAtsPanel(
            state = state,
            jobTitle = jobTitle,
            company = company,
            jobDescription = jobDescription,
            onJobTitleChange = { jobTitle = it },
            onCompanyChange = { company = it },
            onJobDescriptionChange = { jobDescription = it },
            onRunAnalysis = {
                viewModel.runAtsCheck(jobTitle, company, jobDescription)
            }
        )

        CvBuilderCoverLetterPanel(
            state = state,
            jobTitle = jobTitle,
            company = company,
            onGenerate = {
                viewModel.generateCoverLetter(jobTitle, company)
            }
        )

        Button(
            onClick = { viewModel.saveDocument() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1267E8)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text("Simpan CV", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        exportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pdfPicker.launch("CV.pdf") }, modifier = Modifier.weight(1f)) {
                Text("Ekspor PDF")
            }
            OutlinedButton(onClick = { docxPicker.launch("CV.docx") }, modifier = Modifier.weight(1f)) {
                Text("Ekspor DOCX")
            }
        }
    }
}


@Composable
private fun CvBuilderTemplateSelector(
    templateId: String,
    latexStyle: String,
    onTemplateChange: (String) -> Unit,
    onLatexStyleChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pilih Template", fontWeight = FontWeight.Bold)
        CvBuilderConstants.HTML_TEMPLATE_IDS.forEach { id ->
            OutlinedButton(
                onClick = { onTemplateChange(id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text((if (templateId == id) "✓ " else "") + id.replaceFirstChar { it.uppercase() })
            }
        }
        Text("Gaya: $latexStyle", fontSize = 12.sp)
        OutlinedButton(onClick = {
            onLatexStyleChange(if (latexStyle == "classic") "modern" else "classic")
        }) {
            Text("Ganti Gaya")
        }
    }
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
        state.ats?.let { Text("Hasil ATS: $it") }
        state.keyword?.let { Text("Kecocokan kata kunci: $it") }
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
        state.coverLetter?.let { Text(it.toString()) }
    }
}
