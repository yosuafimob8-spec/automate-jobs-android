package com.example.automatesmartbot.data.career

import java.time.LocalDate

object CareerLetters {
    fun resignation(name: String, company: String, position: String, noticeDate: LocalDate, lastDay: LocalDate, reason: String): String {
        require(name.isNotBlank() && company.isNotBlank() && position.isNotBlank()) { "Nama, perusahaan, dan jabatan wajib diisi." }
        require(!lastDay.isBefore(noticeDate)) { "Hari kerja terakhir tidak boleh sebelum tanggal surat." }
        return """$noticeDate

Perihal: Pengunduran Diri

Yth. Manajemen $company

Dengan hormat,

Saya, $name, yang bekerja sebagai $position, mengajukan pengunduran diri dengan hari kerja terakhir yang saya usulkan pada $lastDay.

${reason.trim().takeIf { it.isNotEmpty() }?.let { "Alasan pengunduran diri: $it\n\n" }.orEmpty()}Saya berterima kasih atas kesempatan dan pengalaman yang diberikan. Saya bersedia membantu proses serah terima pekerjaan sesuai kesepakatan dan ketentuan yang berlaku.

Mohon konfirmasi atas usulan tanggal tersebut dan proses administrasi yang diperlukan.

Hormat saya,

$name"""
    }
}
