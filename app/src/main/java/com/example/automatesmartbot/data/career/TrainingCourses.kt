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
        ,TrainingCourse("warehouse", "Logistik & Warehouse", listOf(
            "Keselamatan gudang" to "Gunakan APD sesuai risiko, pahami jalur evakuasi, dan pisahkan area pejalan kaki dari alat angkut.",
            "Penerimaan barang" to "Cocokkan surat jalan dengan pesanan, periksa jumlah dan kondisi, lalu catat selisih sebelum barang disimpan.",
            "Penyimpanan dan FIFO" to "Atur lokasi rak, beri label SKU, gunakan FIFO atau FEFO sesuai karakter barang, dan catat perpindahan stok.",
            "Picking dan packing" to "Ambil barang berdasarkan picking list, periksa SKU dan jumlah, kemas sesuai risiko kerusakan, lalu beri label.",
            "Stock opname" to "Hitung fisik, bandingkan dengan sistem, investigasi selisih, dan lakukan penyesuaian hanya dengan persetujuan."
        ), listOf(
            TrainingQuestion("Saat barang diterima, langkah pertama?", listOf("Langsung simpan", "Cocokkan dokumen dan kondisi", "Buang surat jalan"), 1),
            TrainingQuestion("FIFO berarti?", listOf("Barang masuk lebih dulu keluar lebih dulu", "Barang terbaru selalu keluar dulu", "Stok tanpa label"), 0),
            TrainingQuestion("Jika stok fisik berbeda dengan sistem?", listOf("Ubah diam-diam", "Abaikan", "Investigasi dan laporkan"), 2),
            TrainingQuestion("Picking list digunakan untuk?", listOf("Menentukan barang yang diambil", "Menghapus stok", "Mengatur gaji"), 0),
            TrainingQuestion("APD digunakan berdasarkan?", listOf("Warna seragam", "Risiko pekerjaan", "Selera pribadi"), 1)
        )),
        TrainingCourse("administration", "Administrasi Perkantoran", listOf(
            "Dokumen kerja" to "Kelola surat, formulir, dan arsip dengan penamaan konsisten dan hak akses yang tepat.",
            "Spreadsheet dasar" to "Gunakan tabel, filter, validasi data, dan rumus sederhana untuk rekap operasional.",
            "Komunikasi profesional" to "Tuliskan tujuan pesan dengan jelas, gunakan subjek yang tepat, dan pastikan lampiran benar."
        ), listOf(
            TrainingQuestion("Dokumen rahasia sebaiknya?", listOf("Dibagikan bebas", "Diberi akses sesuai kebutuhan", "Diupload ke publik"), 1),
            TrainingQuestion("Filter spreadsheet berguna untuk?", listOf("Menyaring data", "Menghapus semua data", "Mengganti kata sandi"), 0),
            TrainingQuestion("Email profesional perlu?", listOf("Subjek jelas", "Tanpa penerima", "Lampiran salah"), 0),
            TrainingQuestion("Sebelum kirim laporan?", listOf("Cek data", "Asal kirim", "Hilangkan tanggal"), 0),
            TrainingQuestion("Arsip sebaiknya?", listOf("Tidak diberi nama", "Disimpan acak", "Diberi nama konsisten"), 2)
        )),
        TrainingCourse("sales", "Sales & Marketing", listOf(
            "Memahami pelanggan" to "Kenali kebutuhan pelanggan dan cocokkan manfaat produk dengan masalah yang ingin diselesaikan.",
            "Komunikasi penjualan" to "Ajukan pertanyaan terbuka, jelaskan manfaat tanpa klaim berlebihan, dan dengarkan keberatan.",
            "Tindak lanjut" to "Catat hasil percakapan, tindak lanjuti sesuai izin pelanggan, dan perbarui status peluang."
        ), listOf(
            TrainingQuestion("Langkah awal penjualan?", listOf("Pahami kebutuhan", "Paksa membeli", "Abaikan pelanggan"), 0),
            TrainingQuestion("Klaim produk harus?", listOf("Dilebihkan", "Akurat dan bisa dibuktikan", "Dikarang"), 1),
            TrainingQuestion("Keberatan pelanggan sebaiknya?", listOf("Didengarkan", "Diabaikan", "Dibalas marah"), 0),
            TrainingQuestion("Follow-up harus?", listOf("Spam tiap jam", "Tanpa catatan", "Relevan dan menghormati izin"), 2),
            TrainingQuestion("Catatan peluang digunakan untuk?", listOf("Melacak tindak lanjut", "Membocorkan data", "Menghapus riwayat"), 0)
        )),
        TrainingCourse("customer_service", "Pelayanan Pelanggan", listOf(
            "Mendengarkan aktif" to "Dengarkan keluhan sampai selesai, rangkum masalah, dan konfirmasi kebutuhan pelanggan.",
            "Penanganan keluhan" to "Tetap sopan, kumpulkan fakta, jelaskan solusi realistis dan waktu tindak lanjut.",
            "Eskalasi" to "Alihkan masalah ke pihak berwenang jika di luar kewenangan, sambil mencatat kronologi."
        ), listOf(
            TrainingQuestion("Saat pelanggan mengeluh?", listOf("Potong pembicaraan", "Dengarkan dan konfirmasi", "Abaikan"), 1),
            TrainingQuestion("Janji solusi sebaiknya?", listOf("Realistis", "Tidak mungkin", "Tanpa batas waktu"), 0),
            TrainingQuestion("Masalah di luar kewenangan?", listOf("Sembunyikan", "Eskalasi sesuai prosedur", "Hapus bukti"), 1),
            TrainingQuestion("Catatan keluhan berisi?", listOf("Fakta dan kronologi", "Rumor", "Data palsu"), 0),
            TrainingQuestion("Komunikasi pelanggan harus?", listOf("Kasar", "Menyalahkan", "Sopan dan jelas"), 2)
        ))
    )
    fun score(answers: List<Int?>, questions: List<TrainingQuestion>): Int {
        require(questions.isNotEmpty() && answers.size == questions.size)
        return answers.zip(questions).count { (a, q) -> a == q.correct } * 100 / questions.size
    }
}
