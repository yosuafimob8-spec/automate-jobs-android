package com.example.automatesmartbot.data.cv

object CvWizard {
    val steps = listOf("Informasi Pribadi", "Objektif / Ringkasan", "Pengalaman", "Pendidikan", "Keterampilan", "Sertifikasi", "Template", "Preview CV", "Ekspor PDF")
    val labels = listOf("Informasi pribadi", "Ringkasan profesional", "Pengalaman kerja", "Pendidikan", "Keterampilan", "Sertifikasi & tambahan", "Pilih template", "Preview", "Ekspor PDF")
    fun errors(page: String, cv: CvContent): Map<String, String> = buildMap {
        if (page == steps[0]) {
            if (cv.contact.fullName.isBlank()) put("name", "Nama lengkap wajib diisi.")
            if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(cv.contact.email.trim())) put("email", "Isi alamat email yang valid.")
            if (cv.contact.phone.count(Char::isDigit) !in 8..15) put("phone", "Isi nomor telepon 8–15 digit.")
            if (cv.contact.location.isBlank()) put("location", "Domisili wajib diisi.")
        }
        if (page == steps[2] && cv.experience.any { (it.title.isNotBlank() || it.company.isNotBlank() || it.bullets.any(String::isNotBlank)) && (it.title.isBlank() || it.company.isBlank()) }) put("experience", "Lengkapi jabatan dan perusahaan pada pengalaman yang diisi.")
        if (page == steps[3] && cv.education.any { (it.degree.isNotBlank() || it.field.isNotBlank() || it.details.any(String::isNotBlank)) && it.school.isBlank() }) put("education", "Lengkapi institusi pada pendidikan yang diisi.")
        if (page == steps[5] && cv.certifications.any { (it.issuer.isNotBlank() || it.date.isNotBlank()) && it.name.isBlank() }) put("certification", "Lengkapi nama sertifikat yang diisi.")
    }
}
