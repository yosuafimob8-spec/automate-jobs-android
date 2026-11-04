package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.components.AppInput
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.ui.theme.karirButtonColors

@Composable
internal fun CvEntriesEditor(section: String, draft: CvContent, update: ((CvContent) -> CvContent) -> Unit) {
    val buttonColors = karirButtonColors()
    val buttonShape = RoundedCornerShape(12.dp)

    when (section) {
        "Pengalaman" -> {
            draft.experience.forEach { e ->
                fun change(next: CvExperience) = update { c -> c.copy(experience = c.experience.map { if (it.id == e.id) next else it }) }
                EntryCard {
                    AppInput("Jabatan", e.title, { change(e.copy(title = it)) })
                    AppInput("Perusahaan", e.company, { change(e.copy(company = it)) })
                    AppInput("Lokasi", e.location, { change(e.copy(location = it)) })
                    AppInput("Mulai (YYYY-MM)", e.start, { change(e.copy(start = it)) })
                    Row { Checkbox(e.current, { change(e.copy(current = it)) }, colors = CheckboxDefaults.colors(checkedColor = KarirOrange)); Text("Masih bekerja di sini", Modifier.padding(top = 12.dp)) }
                    if (!e.current) AppInput("Selesai (YYYY-MM)", e.end, { change(e.copy(end = it)) })
                    AppInput("Tanggung jawab dan pencapaian (satu per baris)", e.bullets.joinToString("\n"), { change(e.copy(bullets = it.lines())) }, minLines = 4)
                    TextButton(onClick = { update { it.copy(experience = it.experience.filter { item -> item.id != e.id }) } }) { Text("Hapus pengalaman", color = MaterialTheme.colorScheme.error) }
                }
            }
            Button(onClick = { update { it.copy(experience = it.experience + CvExperience(id = newCvId())) } }, colors = buttonColors, shape = buttonShape) { Text("Tambah Pengalaman", fontWeight = FontWeight.Bold) }
        }
        "Pendidikan" -> {
            draft.education.forEach { e ->
                fun change(next: CvEducation) = update { c -> c.copy(education = c.education.map { if (it.id == e.id) next else it }) }
                EntryCard {
                    AppInput("Sekolah / Universitas", e.school, { change(e.copy(school = it)) })
                    AppInput("Jenjang pendidikan", e.degree, { change(e.copy(degree = it)) })
                    AppInput("Jurusan", e.field, { change(e.copy(field = it)) })
                    AppInput("Lokasi", e.location, { change(e.copy(location = it)) })
                    AppInput("Mulai", e.start, { change(e.copy(start = it)) })
                    AppInput("Selesai", e.end, { change(e.copy(end = it)) })
                    AppInput("IPK (opsional)", e.gpa, { change(e.copy(gpa = it)) })
                    AppInput("Keterangan (satu per baris)", e.details.joinToString("\n"), { change(e.copy(details = it.lines())) }, minLines = 3)
                    TextButton(onClick = { update { it.copy(education = it.education.filter { item -> item.id != e.id }) } }) { Text("Hapus pendidikan", color = MaterialTheme.colorScheme.error) }
                }
            }
            Button(onClick = { update { it.copy(education = it.education + CvEducation(id = newCvId())) } }, colors = buttonColors, shape = buttonShape) { Text("Tambah Pendidikan", fontWeight = FontWeight.Bold) }
        }
        "Keterampilan" -> {
            draft.skills.forEach { e ->
                fun change(next: CvSkillGroup) = update { c -> c.copy(skills = c.skills.map { if (it.id == e.id) next else it }) }
                EntryCard {
                    AppInput("Kategori keterampilan", e.category, { change(e.copy(category = it)) })
                    AppInput("Keterampilan (satu per baris)", e.items.joinToString("\n"), { change(e.copy(items = it.lines())) }, minLines = 3)
                    TextButton(onClick = { update { it.copy(skills = it.skills.filter { item -> item.id != e.id }) } }) { Text("Hapus kategori", color = MaterialTheme.colorScheme.error) }
                }
            }
            Button(onClick = { update { it.copy(skills = it.skills + CvSkillGroup(id = newCvId())) } }, colors = buttonColors, shape = buttonShape) { Text("Tambah Keterampilan", fontWeight = FontWeight.Bold) }
        }
        "Detail Proyek" -> {
            draft.projects.forEach { e ->
                fun change(next: CvProject) = update { c -> c.copy(projects = c.projects.map { if (it.id == e.id) next else it }) }
                EntryCard {
                    AppInput("Nama proyek", e.name, { change(e.copy(name = it)) })
                    AppInput("Teknologi / keterampilan", e.tech, { change(e.copy(tech = it)) })
                    AppInput("Tautan", e.link, { change(e.copy(link = it)) })
                    AppInput("Mulai", e.start, { change(e.copy(start = it)) })
                    AppInput("Selesai", e.end, { change(e.copy(end = it)) })
                    AppInput("Deskripsi (satu per baris)", e.bullets.joinToString("\n"), { change(e.copy(bullets = it.lines())) }, minLines = 4)
                    TextButton(onClick = { update { it.copy(projects = it.projects.filter { item -> item.id != e.id }) } }) { Text("Hapus proyek", color = MaterialTheme.colorScheme.error) }
                }
            }
            Button(onClick = { update { it.copy(projects = it.projects + CvProject(id = newCvId())) } }, colors = buttonColors, shape = buttonShape) { Text("Tambah Proyek", fontWeight = FontWeight.Bold) }
        }
        "Sertifikasi" -> {
            draft.certifications.forEach { e ->
                fun change(next: CvCertification) = update { c -> c.copy(certifications = c.certifications.map { if (it.id == e.id) next else it }) }
                EntryCard {
                    AppInput("Nama sertifikasi", e.name, { change(e.copy(name = it)) })
                    AppInput("Penerbit", e.issuer, { change(e.copy(issuer = it)) })
                    AppInput("Tanggal", e.date, { change(e.copy(date = it)) })
                    AppInput("Tautan", e.link, { change(e.copy(link = it)) })
                    TextButton(onClick = { update { it.copy(certifications = it.certifications.filter { item -> item.id != e.id }) } }) { Text("Hapus sertifikasi", color = MaterialTheme.colorScheme.error) }
                }
            }
            Button(onClick = { update { it.copy(certifications = it.certifications + CvCertification(id = newCvId())) } }, colors = buttonColors, shape = buttonShape) { Text("Tambah Sertifikasi", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EntryCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFFFDEC9))),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
