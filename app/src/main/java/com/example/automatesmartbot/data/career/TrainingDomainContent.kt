package com.example.automatesmartbot.data.career

/** Domain-specific scenarios and working documents for each profession family. */
object TrainingDomainContent {
    data class Domain(val tool: String, val workflow: String, val record: String, val incident: String, val metric: String)
    private val domains = mapOf(
        "Logistik & Warehouse" to Domain("WMS, barcode scanner, SKU, rak lokasi", "receiving, putaway, picking, packing, dispatch", "surat jalan, kartu stok, berita acara selisih", "barang rusak atau stok fisik berbeda dengan WMS", "akurasi stok, ketepatan picking, lead time"),
        "Administrasi Perkantoran" to Domain("spreadsheet, email, sistem arsip", "terima permintaan, validasi data, proses dokumen, arsip", "notulen, register surat, formulir persetujuan", "dokumen ganda atau data penerima tidak sesuai", "akurasi data, SLA dokumen, waktu proses"),
        "Sales & Marketing" to Domain("CRM, katalog, quotation, pipeline", "prospek, kualifikasi, presentasi, penawaran, tindak lanjut", "catatan kunjungan, quotation, status peluang", "keberatan pelanggan atau klaim produk tidak akurat", "conversion rate, nilai pipeline, repeat order"),
        "Customer Service" to Domain("ticketing, FAQ, CRM, kanal layanan", "terima keluhan, verifikasi, solusi, eskalasi, penutupan", "nomor tiket, kronologi, SLA, hasil resolusi", "pelanggan marah atau tiket melewati SLA", "first response time, resolution time, CSAT"),
        "Human Resources" to Domain("ATS, HRIS, formulir karyawan", "permintaan tenaga kerja, seleksi, administrasi, onboarding", "data kandidat, kontrak, absensi, berkas personalia", "data kandidat bocor atau dokumen belum lengkap", "time to hire, ketepatan payroll, retensi"),
        "Finance & Accounting" to Domain("spreadsheet keuangan, bukti transaksi, jurnal", "verifikasi dokumen, pencatatan, rekonsiliasi, persetujuan", "invoice, kuitansi, jurnal, laporan rekonsiliasi", "selisih kas atau invoice duplikat", "akurasi rekonsiliasi, aging piutang, ketepatan laporan"),
        "Produksi & Manufaktur" to Domain("work instruction, mesin produksi, check sheet", "persiapan line, produksi, pemeriksaan, serah terima", "batch record, hasil QC, downtime log", "produk cacat atau mesin berhenti mendadak", "yield, defect rate, OEE"),
        "Teknik & Maintenance" to Domain("work order, multimeter, checklist inspeksi", "inspeksi, isolasi energi, perbaikan, uji fungsi", "maintenance log, work order, riwayat suku cadang", "kerusakan berulang atau bahaya listrik", "MTTR, MTBF, kepatuhan preventive maintenance"),
        "IT & Digital" to Domain("ticketing IT, repositori kode, log sistem", "triase masalah, reproduksi, perbaikan, pengujian, rilis", "tiket insiden, changelog, hasil pengujian", "akses tidak sah atau layanan tidak tersedia", "uptime, lead time perbaikan, jumlah insiden"),
        "Retail & Store Operations" to Domain("POS, barcode, stok toko", "buka toko, display, transaksi, stok, tutup kas", "laporan penjualan, retur, kas harian", "selisih kas atau barang hilang", "sales per shift, shrinkage, akurasi kas"),
        "Food & Beverage" to Domain("resep standar, termometer, POS, checklist sanitasi", "mise en place, produksi, pelayanan, sanitasi", "catatan suhu, bahan baku, pesanan, waste", "alergen tidak tercatat atau suhu penyimpanan tidak aman", "food cost, waste, waktu layanan"),
        "Transportasi & Pengiriman" to Domain("surat jalan, aplikasi rute, POD, GPS", "cek kendaraan, muat, rute, antar, bukti terima", "POD, log perjalanan, checklist kendaraan", "alamat salah atau keterlambatan pengiriman", "on-time delivery, biaya per rute, keberhasilan antar"),
        "Security & General Affairs" to Domain("buku tamu, CCTV sesuai izin, checklist fasilitas", "patroli, pemeriksaan, pencatatan, eskalasi", "log kejadian, jadwal fasilitas, daftar inventaris", "akses tidak sah atau fasilitas rusak", "waktu respons, penyelesaian tiket fasilitas"),
        "Hospitality & Perhotelan" to Domain("PMS, daftar reservasi, checklist kamar", "reservasi, check-in, layanan tamu, check-out", "guest folio, room status, laporan housekeeping", "komplain kebersihan atau double booking", "occupancy, waktu kesiapan kamar, guest satisfaction"),
        "Soft Skills & Pengembangan Karier" to Domain("CV, kalender, catatan STAR, target belajar", "rencana, latihan, umpan balik, perbaikan", "CV, catatan wawancara, rencana pengembangan", "jawaban tidak relevan atau target belajar terlewat", "ketercapaian target, kualitas latihan, konsistensi")
    )
    fun forCategory(category: String): Domain = requireNotNull(domains[category]) { "Bidang tidak dikenal: $category" }
    fun categories(): Set<String> = domains.keys
}
