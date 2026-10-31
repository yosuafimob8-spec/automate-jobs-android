package com.example.automatesmartbot.data.career

data class TrainingQuestion(val prompt: String, val options: List<String>, val correct: Int)
data class TrainingCourse(val id: String, val title: String, val lessons: List<Pair<String, String>>, val questions: List<TrainingQuestion>)
object TrainingCourses {
    val all = listOf(
        TrainingCourse("job_readiness", "Persiapan Melamar Kerja", listOf(
            "Membaca lowongan" to "Cocokkan persyaratan wajib, lokasi, dan batas waktu dengan profil Anda. Periksa sumber perusahaan dan hindari permintaan biaya rekrutmen yang tidak dapat diverifikasi.",
            "Menyusun CV" to "Gunakan kontak aktif, pengalaman nyata, periode pendidikan, dan keterampilan yang dikuasai. Tulis pencapaian secara spesifik; jangan membuat angka atau pengalaman fiktif.",
            "Email profesional" to "Gunakan subjek posisi yang jelas, sapaan sopan, uraian relevansi singkat, dan lampiran PDF yang telah diperiksa. Kirim hanya ke alamat rekrutmen yang memiliki sumber."
        ), listOf(
            TrainingQuestion("Sumber lowongan yang perlu diperiksa pertama?", listOf("Situs karier resmi perusahaan", "Pesan berantai tanpa sumber", "Akun anonim"), 0),
            TrainingQuestion("Pernyataan CV yang tepat?", listOf("Mengarang pengalaman agar cocok", "Mencantumkan pengalaman yang benar-benar dimiliki", "Menyalin CV orang lain"), 1),
            TrainingQuestion("Subjek email lamaran sebaiknya?", listOf("Kosong", "TOLONG BACA!!!", "Menyebut posisi dan nama pelamar"), 2),
            TrainingQuestion("Sebelum mengirim PDF, Anda perlu?", listOf("Memeriksa isi, kontak dan keterbacaannya", "Mengirim file rusak", "Menghilangkan kontak"), 0),
            TrainingQuestion("Jika perekrut meminta biaya tidak jelas?", listOf("Langsung transfer", "Verifikasi melalui kanal resmi terlebih dahulu", "Bagikan OTP"), 1)
        )),
        TrainingCourse("interview", "Komunikasi Wawancara", listOf(
            "Persiapan" to "Baca profil perusahaan dan tanggung jawab posisi. Siapkan contoh pengalaman nyata yang relevan serta pertanyaan tentang pekerjaan.",
            "Metode STAR" to "Ceritakan Situation (situasi), Task (tugas), Action (tindakan Anda), dan Result (hasil). Jelaskan kontribusi sendiri tanpa mengambil kredit atas pekerjaan orang lain.",
            "Mendengarkan aktif" to "Dengarkan pertanyaan sampai selesai. Minta klarifikasi jika belum jelas, jawab ringkas, dan sampaikan keterbatasan pengalaman secara jujur."
        ), listOf(
            TrainingQuestion("STAR berarti?", listOf("Situasi, tugas, tindakan, hasil", "Sekadar menyebut jabatan", "Menghafal jawaban palsu"), 0),
            TrainingQuestion("Jika pertanyaan belum jelas?", listOf("Menebak dan mengarang", "Meminta klarifikasi", "Memotong pewawancara"), 1),
            TrainingQuestion("Contoh pengalaman yang dipakai sebaiknya?", listOf("Cerita fiktif", "Pekerjaan orang lain", "Pengalaman nyata Anda"), 2),
            TrainingQuestion("Sebelum wawancara?", listOf("Mempelajari perusahaan dan posisi", "Mengabaikan deskripsi kerja", "Menyebarkan data rahasia"), 0),
            TrainingQuestion("Saat belum menguasai suatu alat?", listOf("Mengaku ahli", "Jelaskan tingkat penguasaan dan rencana belajar", "Menghindari pertanyaan"), 1)
        ))
    )
    fun score(answers: List<Int?>, questions: List<TrainingQuestion>): Int {
        require(questions.isNotEmpty() && answers.size == questions.size)
        return answers.zip(questions).count { (a, q) -> a == q.correct } * 100 / questions.size
    }
}
