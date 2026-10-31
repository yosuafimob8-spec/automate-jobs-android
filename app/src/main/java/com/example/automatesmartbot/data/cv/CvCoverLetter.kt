package com.example.automatesmartbot.data.cv

enum class LetterType(val label: String) {
    STANDARD("Surat Lamaran Standar"), EXPERIENCE("Tonjolkan Pengalaman"),
    SKILLS("Tonjolkan Kemampuan"), AI("Rekomendasi AI")
}
data class CvCoverLetter(
    val id: String, val jobTitle: String, val company: String, val content: String,
    val createdAt: Long, val type: LetterType = LetterType.STANDARD
)

object CvCoverLetterGenerator {
    fun generate(resume: CvContent, jobTitle: String, company: String, type: LetterType = LetterType.STANDARD): CvCoverLetter {
        require(jobTitle.isNotBlank() && company.isNotBlank()) { "Isi posisi dan perusahaan tujuan." }
        require(type != LetterType.AI) { "Integrasi AI belum dikonfigurasi. Pilih jenis surat lain." }
        val content = buildString {
            append("Perihal: Lamaran untuk posisi ${jobTitle.trim()}\n\nYth. Tim Rekrutmen ${company.trim()}\n\nDengan hormat,\n\n")
            append("Saya mengajukan lamaran untuk posisi ${jobTitle.trim()} di ${company.trim()}.")
            if (resume.contact.fullName.isNotBlank()) append(" Nama saya ${resume.contact.fullName.trim()}.")
            append("\n\n")
            if (type != LetterType.SKILLS) {
                if (resume.summary.isNotBlank()) append(resume.summary.trim()).append("\n\n")
                resume.experience.filter { it.title.isNotBlank() || it.company.isNotBlank() || it.bullets.any { b -> b.isNotBlank() } }.take(if (type == LetterType.EXPERIENCE) 3 else 1).forEach { e ->
                    append(listOf(e.title, e.company).filter { it.isNotBlank() }.joinToString(" di ")).append("\n")
                    e.bullets.filter { it.isNotBlank() }.take(3).forEach { append("• ${it.trim()}\n") }
                    append("\n")
                }
            }
            val skills = resume.skills.flatMap { it.items }.filter { it.isNotBlank() }.distinct()
            if (skills.isNotEmpty()) append("Keterampilan yang tercantum dalam CV saya meliputi ${skills.take(if (type == LetterType.SKILLS) 15 else 6).joinToString(", ")}.\n\n")
            if (resume.experience.isEmpty()) resume.education.firstOrNull()?.let { e ->
                val education = listOf(e.degree, e.field, e.school).filter { it.isNotBlank() }.joinToString(" — ")
                if (education.isNotBlank()) append("Latar belakang pendidikan saya: $education.\n\n")
                else if (e.details.isNotEmpty()) append(e.details.joinToString("\n")).append("\n\n")
            }
            append("Saya berharap mendapat kesempatan untuk mendiskusikan kesesuaian latar belakang saya dengan kebutuhan perusahaan. Terima kasih atas waktu dan pertimbangan Bapak/Ibu.\n\nHormat saya,\n")
            append(resume.contact.fullName)
            listOf(resume.contact.email, resume.contact.phone).filter { it.isNotBlank() }.forEach { append("\n$it") }
        }
        return CvCoverLetter(newCvId(), jobTitle.trim(), company.trim(), content.trim(), System.currentTimeMillis(), type)
    }
}
