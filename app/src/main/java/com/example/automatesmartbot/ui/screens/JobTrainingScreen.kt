package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.career.*
import com.example.automatesmartbot.data.cv.CvRenderer
import com.example.automatesmartbot.data.product.ProductPolicy
import com.example.automatesmartbot.ui.components.*
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel

@Composable
fun JobTrainingScreen(cvViewModel: CvBuilderViewModel? = null, isPro: Boolean = false) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val catalog = remember { LearningCatalog.load(context) }
    val store = remember { LearningStore(context) }
    val policy = remember { ProductPolicy(context) }
    val names = remember { context.getSharedPreferences("career_training", 0) }
    var name by rememberSaveable { mutableStateOf(names.getString("name", "").orEmpty().ifBlank { cvViewModel?.state?.value?.draft?.contact?.fullName.orEmpty() }) }
    var page by rememberSaveable { mutableStateOf("catalog") }
    var courseId by rememberSaveable { mutableStateOf(catalog.courses.first().id) }
    var moduleIndex by rememberSaveable { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var reviewed by rememberSaveable(page, courseId, moduleIndex) { mutableStateOf(false) }
    val course = catalog.courses.first { it.id == courseId }
    val progress = remember(courseId, revision) { store.load(courseId) }
    fun update(action: () -> Unit) { try { action(); revision++; message = "" } catch (e: Exception) { message = e.message ?: "Progres belum dapat disimpan." } }
    fun back() { page = when(page) { "module", "simulation", "exam", "results" -> "course"; else -> "catalog" }; message = "" }
    BackHandler(enabled = page != "catalog") { back() }
    if (page == "legacy") { Column(Modifier.fillMaxSize()) { TextButton(onClick = { page = "catalog" }) { Text("Kembali ke Bidang Pelatihan") }; Box(Modifier.weight(1f)) { LegacyJobTrainingScreen() } }; return }
    key(page, courseId, moduleIndex) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (page != "catalog") TextButton(onClick = ::back) { Text("Kembali") }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        when (page) {
            "catalog" -> {
                JourneyHeader("Pelatihan Kerja", "Delapan bidang · materi bertahap, latihan, simulasi dan sertifikat internal")
                Text("Seluruh bidang tersedia untuk FREE dan PRO. Sertifikat KarirMate adalah bukti penyelesaian internal, bukan sertifikasi kompetensi resmi BNSP.")
                OutlinedButton(onClick = { page = "history" }) { Text("Riwayat Pembelajaran & Sertifikat") }
                catalog.courses.forEach { c ->
                    val p = remember(c.id, revision) { store.load(c.id) }
                    Card(onClick = { courseId = c.id; page = "course" }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium)
                        Text("${policy.trainingTier(c.id)?.uppercase() ?: "Akses tersedia"} · ${p.completedModules.size}/${c.modules.size} modul selesai")
                        LinearProgressIndicator(progress = { p.completedModules.size.toFloat() / c.modules.size }, modifier = Modifier.fillMaxWidth())
                        Text(if (p.certificate != null) "Sertifikat tersedia · Lihat hasil" else "Lanjutkan belajar →", color = MaterialTheme.colorScheme.primary)
                    } }
                }
                TextButton(onClick = { page = "legacy" }) { Text("Materi & Sertifikat Versi Sebelumnya") }
            }
            "course" -> {
                val completed = progress.completedModules.size + (if (progress.simulationCompleted) 1 else 0) + (if (progress.certificate != null) 1 else 0)
                JourneyHeader(course.title, "${policy.trainingTier(course.id)?.uppercase() ?: "Akses tersedia"} · progres $completed/${course.modules.size+2}", completed.toFloat()/(course.modules.size+2))
                Text(course.description)
                Text("Materi ditulis untuk belajar mandiri; praktik kerja tetap mengikuti SOP dan pelatihan di tempat kerja.")
                AppInput("Nama peserta *", name, { name = it; names.edit().putString("name", it).apply() })
                course.modules.forEachIndexed { index, module ->
                    val unlocked = course.modules.take(index).all { it.id in progress.completedModules }
                    OutlinedButton(onClick = { moduleIndex = index; page = "module" }, enabled = unlocked, modifier = Modifier.fillMaxWidth()) {
                        Text("${index+1}. ${module.title} ${if (module.id in progress.completedModules) "✓" else ""}")
                    }
                }
                Button(onClick = { page = "simulation" }, enabled = progress.completedModules.size == course.modules.size, modifier = Modifier.fillMaxWidth()) { Text("Simulasi Pekerjaan") }
                Button(onClick = { page = "exam" }, enabled = progress.simulationCompleted, modifier = Modifier.fillMaxWidth()) { Text("Ujian Akhir · ${course.exam.size} Soal") }
                Text("Batas kelulusan saat ini: ${policy.trainingPassScore}/100. Jumlah modul dan batas nilai dapat ditinjau kembali.")
                if (progress.attempts.isNotEmpty()) OutlinedButton(onClick = { page = "results" }) { Text("Lihat Hasil & Pembahasan") }
            }
            "module" -> {
                val module = course.modules[moduleIndex]
                JourneyHeader(module.title, "Modul ${moduleIndex+1}/${course.modules.size}", (moduleIndex+1f)/course.modules.size)
                Text("Tujuan pembelajaran", style = MaterialTheme.typography.titleMedium); Text(module.objective)
                Text(module.content, style = MaterialTheme.typography.bodyLarge)
                Text("Sumber materi", style = MaterialTheme.typography.titleMedium)
                module.references.forEach { ref ->
                    Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(ref.title); Text(ref.publisher); ref.standard?.let { Text(it) }; Text("Diperiksa: ${ref.checkedOn}"); Text(ref.scope, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { runCatching { uriHandler.openUri(ref.url) }.onFailure { message = "Sumber belum dapat dibuka. ${ref.url}" } }) { Text("Periksa Sumber Resmi") }
                    } }
                }
                Text("Latihan Modul", style = MaterialTheme.typography.titleMedium)
                module.practice.forEachIndexed { i, q -> LearningQuestionCard(q, progress.answers["${module.id}_$i"], reviewed || module.id in progress.completedModules) { answer -> update { store.answer(course, "${module.id}_$i", answer) }; reviewed = false } }
                Button(onClick = { update { store.completeModule(course, moduleIndex); reviewed = true } }, modifier = Modifier.fillMaxWidth()) { Text("Periksa & Simpan Latihan") }
                if (reviewed || module.id in progress.completedModules) Button(onClick = { page = "course" }, modifier = Modifier.fillMaxWidth()) { Text("Lanjutkan Pembelajaran") }
            }
            "simulation" -> {
                JourneyHeader("Simulasi Pekerjaan", course.title)
                LearningQuestionCard(course.simulation, progress.answers["simulation"], reviewed || progress.simulationCompleted) { answer -> update { store.answer(course, "simulation", answer) }; reviewed = false }
                Button(onClick = { update { store.completeSimulation(course); reviewed = true } }) { Text("Nilai & Simpan Simulasi") }
                if (reviewed || progress.simulationCompleted) Button(onClick = { page = "exam" }) { Text("Lanjut ke Ujian Akhir") }
            }
            "exam" -> {
                JourneyHeader("Ujian Akhir", "${course.title} · ${course.exam.indices.count { progress.answers["exam_$it"] != null }}/${course.exam.size} terjawab")
                AppInput("Nama peserta *", name, { name = it; names.edit().putString("name", it).apply() })
                course.exam.forEachIndexed { i, q -> LearningQuestionCard(q, progress.answers["exam_$i"], false) { answer -> update { store.answer(course, "exam_$i", answer) } } }
                Button(onClick = { update { store.submit(course, name, policy.trainingPassScore); page = "results" } }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("Selesai & Simpan Hasil Ujian") }
            }
            "results" -> {
                val attempt = progress.attempts.lastOrNull()
                JourneyHeader("Hasil Pembelajaran", "${course.title} · ${attempt?.score ?: 0}/100")
                Text(if ((attempt?.score ?: 0) >= policy.trainingPassScore) "Anda menyelesaikan ujian dengan hasil lulus. Pertahankan latihan penerapannya." else "Tinjau pembahasan lalu ulangi ujian. Riwayat percobaan tetap disimpan.")
                progress.certificate?.let { certificate ->
                    Text(certificate.name); Text("Nomor internal: ${certificate.id}")
                    PdfExportButton(CvRenderer.letterPages(certificate.text()), "KarirMate-Sertifikat-${course.id}.pdf", "Unduh Sertifikat PDF")
                    if (cvViewModel != null) OutlinedButton(onClick = {
                        if (cvViewModel.state.value.activeId == null) cvViewModel.newDocument()
                        cvViewModel.updateContent { cv -> cv.copy(certifications = cv.certifications.filterNot { it.id == certificate.id } + certificate.asCvCertification()) }
                        message = "Sertifikat ditambahkan ke draft CV aktif sebagai pelatihan internal KarirMate."
                    }) { Text("Cantumkan Sertifikat di CV") }
                }
                Text("Pembahasan Jawaban", style = MaterialTheme.typography.titleLarge)
                course.exam.forEachIndexed { i, q -> LearningQuestionCard(q, attempt?.answers?.getOrNull(i), true) {} }
                Button(onClick = { page = "exam" }) { Text("Ulangi Ujian") }
            }
            "history" -> {
                JourneyHeader("Riwayat Pembelajaran", "Progres dan hasil disimpan di perangkat ini.")
                catalog.courses.forEach { c ->
                    val p = store.load(c.id)
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium); Text("${p.completedModules.size}/${c.modules.size} modul · ${p.attempts.size} ujian")
                        p.attempts.forEach { Text("${it.completedOn} · ${it.score}/100") }
                        if (p.attempts.isNotEmpty()) TextButton(onClick = { courseId = c.id; page = "results" }) { Text("Hasil & Sertifikat") }
                    } }
                }
                OutlinedButton(onClick = { page = "legacy" }) { Text("Riwayat & Sertifikat Versi Sebelumnya") }
            }
        }
    } }
}

@Composable
private fun LearningQuestionCard(question: LearningQuestion, selected: Int?, showFeedback: Boolean, onSelect: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(question.prompt, style = MaterialTheme.typography.titleMedium)
        question.options.forEachIndexed { i, option -> Row(Modifier.fillMaxWidth().selectable(selected == i, role = Role.RadioButton, onClick = { if (!showFeedback) onSelect(i) })) {
            RadioButton(selected == i, null, enabled = !showFeedback); Text(option, Modifier.weight(1f).padding(top = 12.dp))
        } }
        if (showFeedback) { Text(if (selected == question.correct) "Jawaban tepat" else "Perlu ditinjau: ${question.options[question.correct]}", color = MaterialTheme.colorScheme.primary); Text(question.explanation) }
    } }
}
