# KarirMate — Laporan Audit, Perbaikan & Restorasi
Tanggal pemeriksaan: 31 Oktober 2026 (UTC)

Status: SIAP REVIEW KODE. Produksi dan build APK masih DITAHAN. Pekerjaan belum dinyatakan selesai secara menyeluruh karena integrasi produksi dan pengujian HP belum terverifikasi.

## 1. Sumber kode dan batas pelaksanaan

Sumber utama adalah GitHub main terbaru yang diambil pada awal pekerjaan ini. Snapshot sepuluh berkas utama Termux sudah tersedia dalam repository backend; inventaris mencatat nama, SHA-256 dan impor tanpa membaca token atau database pengguna. Kesamaan dengan Termux yang sedang berjalan tidak dapat dipastikan tanpa akses ke perangkat tersebut.

- Android: ilhamramdhani763-stack/automate-jobs-android.
- Dasar Android: 2d751b6ec584e3706f758826ce22fe3868f9dc3c.
- Commit implementasi Android: f1664e258d0e9394521c4d3414a18ec07232e16c.
- Backend: ilhamramdhani763-stack/automate-jobs-backend.
- Dasar backend: dbfb30120a302da8a045e2d3e50e9ffa2f6ec193.
- Commit implementasi backend: 42496e044896043f7e03484eb3f9471d1f2b0af0.
- Branch terpisah pada kedua repository: fix/karirmate-restoration.
- Tidak membuat APK, tidak menjalankan workflow APK, tidak merge, tidak mengubah cron, token, konfigurasi produksi atau database produksi, dan tidak mengirim email sungguhan. Seluruh pengiriman dalam tes memakai transport tiruan dan database sementara.

## 2. Temuan, akar masalah, dan perbaikan

### Auto Apply dan pesan kandidat

Penyebab pada kode terverifikasi: auto_apply_api.py sebelumnya menggabungkan CV tidak tersedia, teks CV gagal dibaca, profil tidak lengkap, dan kontak tidak memenuhi syarat menjadi daftar kosong dengan catatan. KirimLamaranScreen.kt membuang catatan tersebut dan menampilkan pesan umum. Skema perusahaan yang tidak memenuhi kolom yang dibutuhkan juga dilewati tanpa penjelasan. RiwayatScreen.kt bahkan masih statis.

Literal “Kandidat Tidak Tersedia” tidak ditemukan pada source utama saat audit; variasi label pada APK pengguna belum dapat diperiksa. Kondisi data pasti pada akun produksi belum terbukti. Laporan ini membedakan akar masalah kode yang terbukti dari diagnosis akun produksi yang masih harus dibaca setelah pemasangan yang disetujui.

- auto_apply_diagnostics.py memisahkan CANDIDATE_NOT_FOUND, PROFILE_INCOMPLETE, CV_NOT_AVAILABLE, NO_MATCHING_VACANCY, NO_ELIGIBLE_RECIPIENT, ALL_RECIPIENTS_ALREADY_CONTACTED, serta BACKEND_ERROR. Status menambahkan blocker PRODUCTION_SENDING_BLOCKED dan masalah Gmail/persetujuan/jeda beserta tindakan pengguna.
- Identitas tetap berasal dari sesi Android dan pemetaan Telegram yang tersimpan. Pemetaan hilang/tidak valid ditolak; tidak ada kandidat palsu atau ID pengganti.
- Seleksi membaca CV yang benar-benar berhasil diekstrak, posisi/lokasi profil, serta kontak milik pengguna. Lowongan aktif membutuhkan tanda verifikasi, bukti sumber dan kedaluwarsa. Lamaran spontan diberi jenis tersendiri, bukan diklaim sebagai lowongan aktif.
- Kuota efektif mengikuti pengaturan akun dengan batas atas lima per hari dalam WIB. Ledger klaim mencegah pengiriman ulang; transaksi juga mencegah penerima yang sama masuk antrean aktif dua kali. Riwayat lama dipertahankan.
- Kandidat yang sudah diklaim disaring sebelum mengambil lima penerima, sehingga penerima baru berikutnya dapat mengisi kuota.
- POST /auto-apply/simulate hanya menyusun rencana dari data yang tersedia dan kuota tersisa. Tidak memanggil worker, menulis antrean/klaim/riwayat, atau mengubah izin. Hasil selalu menegaskan nol email dikirim.
- Status TERKIRIM membutuhkan Gmail message ID. Timeout/kegagalan transport yang tidak pasti tetap berstatus uncertain, memakai kuota, dan tidak dicoba ulang otomatis. Data lama berstatus sent tanpa ID ditampilkan sebagai belum pasti.
- GET /karirmate/report mendukung hari ini, 7 hari, 30 hari dan semua, paginasi, detail perusahaan/penerima/posisi/waktu/status/error/ID Gmail, rekap harian dan total historis. Android menampilkan data backend dan menolak rekap server lama yang belum lengkap.
- Persetujuan pertama dan timestamp tetap tersimpan ketika dijeda/diaktifkan kembali. Blokir operator dan jadwal yang belum diverifikasi tetap terlihat. Tidak ada tombol yang mengaktifkan izin produksi atau mengubah cron.

### Psikotes

Akar masalah: navigasi dan tombol selesai berada di dalam gulir konten; selesai hanya muncul pada soal terakhir. Layar hasil sebelumnya hanya kartu teks sederhana. Pembatasan 10–20 diterapkan pada semua mode sehingga simulasi penuh ikut terpotong. Analisis PRO belum dibatasi pada kedua jalur respons backend.

- ExamPanel memisahkan area soal yang dapat digulir dari tombol tetap Sebelumnya, Selanjutnya dan Selesaikan Tes. Tombol selesai terlihat sejak soal pertama; konfirmasi menyebut jumlah jawaban kosong. Sebelumnya nonaktif pada awal, dengan tombol oranye dan tulisan putih.
- Progres terjawab, waktu, jawaban lokal tertunda dan sinkronisasi dipertahankan. Hasil tetap berasal dari server dan riwayatnya tidak dihapus.
- PsychotestResultPanel memulihkan skor 1–100, ringkasan, motivasi sesuai hasil, animasi progres ringan, analisis per materi dan pembahasan PRO. Asesmen kepribadian tidak diberi nilai benar/salah palsu.
- Latihan singkat tetap 10–20 soal; simulasi penuh mengirim permintaan tanpa jumlah latihan dan memakai konfigurasi asli bank. Akses penuh, statistik dan analisis mengikuti langganan terverifikasi. FREE tetap dapat memainkan kategori dasar.
- psychotest_access.py menyaring detail PRO pada submit dan result tanpa mengubah hasil tersimpan. Definisi bank dan seluruh mode tidak dihapus.

### CV Builder, template dan analyzer

Akar masalah: autosave telah ada tetapi tahap informasi pribadi dan editor lain tidak memiliki tindakan lanjut yang jelas. Tidak ada checkpoint per dokumen atau pemetaan akses produk. Pemeriksaan CV lokal berbeda dengan jalur analisis PDF, dan judul bagian satu baris tidak dikenali.

- CvWizard memberikan sembilan tahap: informasi pribadi, ringkasan, pengalaman, pendidikan, keterampilan, sertifikasi/tambahan, template, preview, ekspor. Tombol Simpan & Lanjut tetap terlihat, progres tersedia, input pribadi divalidasi pada kolom, dan draft/checkpoint dapat dilanjutkan per dokumen setelah pembukaan ulang.
- Satu panah kembali header dan Back Android tetap menggunakan riwayat yang sama. Kembali juga memperbarui checkpoint. Bagian yang belum dimiliki, misalnya pengalaman atau sertifikasi, boleh dilewati tanpa mengarang data.
- Semua 14 desain dan ID dipertahankan; template dapat diganti tanpa kehilangan formulir. Foto, editor bagian tambahan, urutan bagian, surat lamaran dan DOCX tetap tersedia.
- Keputusan akses pengguna disimpan pada product_policy.json: FREE ats_01, ats_02, minimal_01 dan minimal_02; sepuluh lainnya PRO. Katalog aktual hanya memiliki dua label ATS, sehingga dua desain Minimalis satu kolom tanpa foto ikut kategori ATS melalui konfigurasi. Desain, ID dan kategori Minimalis aslinya tetap ada. Pemilihan/ekspor PRO dibatasi sesuai status akun, tanpa menghapus draft lama.
- Preview dan PDF tetap memakai renderer yang sama, A4, pembagian halaman, margin, teks yang dapat diseleksi, dan foto hanya pada desain yang mendukungnya. Uji regresi memeriksa semua desain, teks terakhir pada dokumen panjang dan foto.
- Analyzer mendukung CV aktif dan document picker PDF eksternal. Analisis memakai teks aktual, termasuk heading inline; menampilkan kontak, profil, pengalaman, pendidikan, keterampilan, kelebihan/kekurangan dan rekomendasi struktur ATS. PDF scan/tidak terbaca menampilkan kegagalan ekstraksi tanpa skor nol palsu. OCR belum tersedia.
- Tiga jenis surat berbasis aturan dan fakta CV tetap berfungsi; opsi AI tetap memberi status jujur bahwa integrasi AI belum tersedia. Tidak ada pengalaman atau kemampuan fiktif.

### Pelatihan Kerja

Akar masalah: implementasi sebelumnya hanya dua kursus singkat, tanpa delapan bidang, referensi per modul, alur bertingkat atau simulasi kasus.

- Delapan bidang tersedia: Administrasi Perkantoran, Warehouse/Pergudangan, Logistik, Customer Service, Sales & Marketing, Operator Produksi, HR & Recruitment, dan Dasar Dunia Kerja.
- Konten awal masing-masing memiliki dua modul dengan judul, tujuan, materi orisinal, referensi, penerbit, nomor standar jika terverifikasi, serta tanggal pemeriksaan. Materi merupakan pengantar internal; tidak diklaim sebagai pemenuhan seluruh unit SKKNI atau pelatihan resmi instansi penerbit.
- Alur modul berurutan, latihan dengan pembahasan, simulasi pekerjaan, ujian akhir lima soal, skor, seluruh percobaan, riwayat dan progres disimpan. Sertifikat PDF memuat nama, bidang, tanggal, nomor internal dan identitas KarirMate, disertai pernyataan bukan sertifikasi kompetensi resmi BNSP.
- Sertifikat dapat dicantumkan ke draft CV aktif sebagai pelatihan internal, dengan ID untuk menghindari duplikasi. Kursus/sertifikat lama tetap dapat dibuka melalui menu versi sebelumnya dan preferensinya tidak direset.
- Seluruh delapan bidang tersedia untuk FREE maupun PRO sesuai keputusan pengguna. Batas kelulusan saat ini 80/100 dan jumlah modul awal tetap konfigurasi yang bisa ditinjau; tidak dianggap keputusan produk permanen.

### UI/UX dan kalkulator

Akar masalah: Dashboard dan Psikotes memaksakan permukaan terang dengan warna teks tetap, dan Dashboard mengumumkan riwayat belum tersedia. Kalkulator hanya menampilkan satu kesalahan umum, serta menggunakan satu basis untuk lembur dan THR.

- Dashboard mengikuti tema, kartu lebih berhierarki, gradasi header, jarak dan tombol lebih jelas. Grid menjadi satu kolom pada lebar/font besar. Navigasi bawah asli tetap ada dan konten mengikuti padding Scaffold.
- JourneyHeader reusable memberi judul, konteks dan progres pada CV/pelatihan. Hasil psikotes memakai komponen tema yang sama. AppInput mempertahankan label di atas input dan warna berdasarkan tema.
- PayrollForm memberi error spesifik tanpa menghapus nilai. Dasar upah lembur dan THR dipisahkan; aritmetika lama dipertahankan. Validasi jam istirahat sistem enam hari (maksimum 11) dan hari terpendek (9) dilengkapi, dengan uji batas.
- Rujukan resmi perhitungan ditampilkan. PPh 21 adalah estimasi tahunan/rata-rata bulanan, bukan potongan TER aktual. Dasar iuran JP tahunan dimasukkan dari slip, tidak ditebak aplikasi. Ketentuan khusus sektor/komponen upah harus dicocokkan dengan dokumen kerja.

## 3. Hasil pengujian

| Pemeriksaan | Hasil | Ruang lingkup |
| --- | --- | --- |
| Android/JVM/Compose | PASS | 91 tes, 0 gagal; Robolectric SDK 35/native, Java 21 |
| Backend | PASS | 46 tes, 0 gagal; SQLite/PDF sementara, transport mock, HTTP loopback |
| Python kontrak Android | PASS | 13 tes, 0 gagal; layanan bank sintetis dan kebijakan |
| Kotlin utama | PASS | compileDebugKotlin |
| Kotlin tes perangkat | PASS | compileDebugAndroidTestKotlin; kompilasi saja |
| Android lint | PASS dengan peringatan | 0 error, 71 warning; bukan bebas peringatan |
| Sintaks Python | PASS | Parsing/kompilasi berkas sumber; bukan startup seluruh aplikasi |
| Patch dan bundle | PASS | git apply --check pada indeks dasar dan git bundle verify |
| Capture gambar Robolectric | FAIL / keterbatasan harness | WindowCapture redraw timeout; helper capture tambahan dihapus, tes perilaku dipertahankan lalu suite penuh lulus |
| Pengujian HP/emulator dan tampilan visual nyata | NOT TESTED | Tidak memasang/membangun APK |
| Gmail OAuth/refresh/worker produksi | NOT TESTED | Worker eksternal dan konfigurasi produksi tidak dijalankan |
| Pengiriman email nyata/cron | NOT TESTED | Dilarang pengguna; tidak diaktifkan |

Cakupan tes meliputi navigasi bertahap, draft/foto persisten, 14 preview, A4/PDF multipage, generator surat, analisis PDF aktual/gagal, tombol selesai pada layar kecil, nilai dan akses PRO, delapan alur pelatihan/sertifikat, validasi gaji, identitas kandidat, profil/CV/Gmail kosong, spontan, duplikat, kuota lima, paralelisme, kegagalan/konfirmasi Gmail, laporan/filter dan pemisahan akun. Fixture bukan data pengguna atau hasil produksi.

Kegagalan saat pengembangan juga dicatat: satu kesalahan kurung Kotlin diperbaiki; dua tes daftar hasil perlu menggulir melalui indeks LazyColumn; capture frame gagal pada lingkungan Robolectric. Suite final dijalankan kembali setelah perbaikan, tanpa menonaktifkan tes perilaku.

## 4. Audit regresi dan risiko

- CV lama, foto, surat, PDF karier, ekspor multipage, navigasi bawah dan riwayat latihan lama diperiksa melalui tes penyimpanan/renderer/navigasi. Data tidak dimigrasi dengan reset atau penghapusan.
- Login/registrasi, profil, Surat Izin & Dokumen Kerja dan layanan API lama dipertahankan jalurnya. Tes kebijakan sesi, profil dan graph navigasi lulus; operasi penuh dengan akun produksi belum diuji.
- Risiko utama: ketidaklengkapan modul backend, kontrak worker Gmail yang belum diverifikasi, skema kontak tanpa lokasi/bukti, viewer PDF/izin picker yang berbeda antar-HP, dan review tampilan dengan font besar/tema pada perangkat sebenarnya.
- Kebijakan anti-duplikat bersifat konservatif per penerima. Status belum pasti tidak dikirim ulang otomatis; operator harus memeriksa Gmail sebelum keputusan lanjutan.
- Draft PRO tetap tersimpan jika akun turun paket; akses penggunaan/ekspor desain PRO mengikuti langganan. Empat desain FREE tetap tersedia.
- Materi awal pelatihan dan batas kelulusan perlu review editorial/produk lanjutan. Sertifikat tidak menjanjikan lisensi profesi, kelulusan BNSP atau pekerjaan.

## 5. Fitur yang belum selesai dan dependensi spesifik

Integrasi produksi belum dapat dinyatakan berfungsi ujung ke ujung. Sepuluh source utama ada, tetapi source berikut masih diperlukan dari lingkungan Termux/worker, tanpa token atau database pribadi:

- Android/auth/keanggotaan: android_auth.py, account_access_policy.py, karirmate_integrasi_final.py.
- Psikotes dan identitas pencarian: search_identity.py, psychotest_bank.py, psychotest_skb.py. Bank asli tidak diganti dengan bank palsu.
- Layanan pekerjaan/bot mengimpor: application_methods, company_catalog, company_search, job_catalog, job_validation, live_sources, search_engine, idjobs_feed, github_search_engine, kalibrr_client, local_jobs, phase4_watch dan storage.
- Diperlukan antarmuka konfigurasi tersanitasi untuk config.py; nilai BOT_TOKEN, key Gmail dan kredensial lain harus tetap di lingkungan produksi, bukan GitHub.
- Worker eksternal /api/gmail/send-application belum tersedia sourcenya. Refresh token, scope, idempotency, kepastian message_id dan dukungan eksplisit surat spontan perlu ditinjau pada worker nyata.
- Data perusahaan harus memiliki posisi, lokasi dan sumber kontak; lowongan aktif harus punya bukti dan masa berlaku. Source tidak mengisi lokasi/email yang hilang dengan tebakan.
- Paket pihak ketiga tercantum dalam termux/requirements.txt: requests, pypdf dan python-telegram-bot. Tes hanya memasang/memakai dependensi yang diperlukan dalam lingkungan terpisah.
- OCR PDF scan dan integrasi AI generator surat belum tersedia. Keduanya tetap dilaporkan jujur dan tidak menghasilkan simulasi yang diklaim sebagai analisis/AI nyata.

## 6. Daftar berkas diubah

Daftar berikut merupakan isi commit implementasi, termasuk dokumen review. Berkas paket unduhan ditambahkan kemudian sebagai commit dokumentasi terpisah.

### Android (32 berkas)

- app/src/main/assets/product_policy.json
- app/src/main/assets/training_catalog.json
- app/src/main/java/com/example/automatesmartbot/data/autoapply/AutoApplyReports.kt
- app/src/main/java/com/example/automatesmartbot/data/career/PayrollCalculator.kt
- app/src/main/java/com/example/automatesmartbot/data/career/PayrollForm.kt
- app/src/main/java/com/example/automatesmartbot/data/career/TrainingLearning.kt
- app/src/main/java/com/example/automatesmartbot/data/cv/CvWizard.kt
- app/src/main/java/com/example/automatesmartbot/data/cv/PdfCvAnalyzer.kt
- app/src/main/java/com/example/automatesmartbot/data/network/ApiService.kt
- app/src/main/java/com/example/automatesmartbot/data/product/ProductPolicy.kt
- app/src/main/java/com/example/automatesmartbot/ui/components/JourneyHeader.kt
- app/src/main/java/com/example/automatesmartbot/ui/components/PayrollSources.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/CareerToolsScreens.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/CvBuilderScreen.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/DashboardScreen.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/JobTrainingScreen.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/KirimLamaranScreen.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/LatihanPsikotesScreen.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/MainDashboardScreen.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/PsychotestResultPanel.kt
- app/src/main/java/com/example/automatesmartbot/ui/screens/RiwayatScreen.kt
- app/src/main/java/com/example/automatesmartbot/ui/viewmodel/CvBuilderViewModel.kt
- app/src/main/java/com/example/automatesmartbot/ui/viewmodel/PsychotestViewModel.kt
- app/src/test/java/com/example/automatesmartbot/data/autoapply/AutoApplyReportsTest.kt
- app/src/test/java/com/example/automatesmartbot/data/career/LearningStoreTest.kt
- app/src/test/java/com/example/automatesmartbot/data/cv/RestorationFlowTest.kt
- app/src/test/java/com/example/automatesmartbot/ui/screens/FeatureUiTest.kt
- app/src/test/java/com/example/automatesmartbot/ui/screens/RestorationUiTest.kt
- app/src/test/java/com/example/automatesmartbot/ui/viewmodel/PsychotestViewModelTest.kt
- backend-contract/psychotest_access.py
- backend-contract/psychotest_api.py
- docs/restoration/REVIEW.md

### Backend (13 berkas)

- docs/restoration/REVIEW.md
- termux/automate-ai/auto_apply_api.py
- termux/automate-ai/auto_apply_diagnostics.py
- termux/automate-ai/gmail_oauth_client.py
- termux/automate-ai/phase511_controls.py
- termux/automate-ai/phase512_sender.py
- termux/automate-ai/phase57_queue.py
- termux/automate-jobs-v2/psychotest_access.py
- termux/automate-jobs-v2/psychotest_api.py
- tests/test_auto_apply_restoration.py
- tests/test_psychotest_access.py
- tests/test_psychotest_api_access.py
- tests/test_termux_auto_apply.py

## 7. Referensi dan konfigurasi yang dapat diperiksa

Sumber diperiksa 31 Oktober 2026. Referensi dicantumkan kembali pada setiap modul di training_catalog.json; tidak ada nomor unit kompetensi yang dibuat sendiri.

- [Administratif Profesional — Kementerian Ketenagakerjaan RI — Kepmenaker 109 Tahun 2024](https://jdih.kemnaker.go.id/peraturan/detail/2498/keputusan-menaker-nomor-109-tahun-2024).
- [SKKNI Bidang Logistik — Kementerian Ketenagakerjaan RI — Kepmenaker 170 Tahun 2020](https://jdih.kemnaker.go.id/peraturan/detail/1947/keputusan-menaker-nomor-170-tahun-2020).
- [Program Pelayanan Pelanggan — Kementerian Ketenagakerjaan RI](https://proglat.kemnaker.go.id/programs/f2a9a86f-5b52-4e2c-9cf1-49905454ea5f/versions/3).
- [Program Content Marketing — Skillhub Kemnaker / UPTD Pelatihan Kerja Disnaker Kabupaten Bandung](https://skillhub.kemnaker.go.id/pelatihan/vokasi-nasional/jadwal/content-marketing-6391d127-8563-4127-ac8a-d660a9cccf4e).
- [Keselamatan Kerja — Pemerintah RI / JDIH Kemnaker — UU 1 Tahun 1970](https://jdih.kemnaker.go.id/peraturan/detail/49/undang-undang-nomor-1-tahun-1970).
- [SKKNI Manajemen Sumber Daya Manusia — Kementerian Ketenagakerjaan RI — Kepmenaker 149 Tahun 2020](https://jdih.kemnaker.go.id/peraturan/detail/1933/keputusan-menaker-nomor-149-tahun-2020).
- [Waktu Kerja, Waktu Istirahat dan Hubungan Kerja — Pemerintah RI / JDIH Kemnaker — PP 35 Tahun 2021](https://jdih.kemnaker.go.id/peraturan/detail/1723/peraturan-pemerintah-nomor-35-tahun-2021).
- [Tugas dan Fungsi BNSP — BNSP](https://ppid.bnsp.go.id/daftar-informasi-publik/informasi-berkala/tugas-dan-fungsi).

- [THR — Permenaker 6/2016, JDIH BPK](https://peraturan.bpk.go.id/Details/146101/permenaker-no-6-tahun-2016).
- [Kalkulator PPh resmi DJP](https://kalkulator.pajak.go.id/).
- [Iuran JHT/JP — BPJS Ketenagakerjaan](https://www.bpjsketenagakerjaan.go.id/en/penerima-upah.html).
- [Basis JKN — Direktorat Jenderal Anggaran, Kemenkeu](https://anggaran.kemenkeu.go.id/in/post/perpres-64-2020-upaya-pemerintah-perbaiki-sistem-jaminan-kesehatan-nasional).

## 8. Paket review dan status build APK

Paket terdiri dari PDF ini, laporan Markdown, patch Android, patch backend, dua Git bundle inkremental, test-results.json, change-inventory.json, source-inventory.json, README unduhan, dan SHA256SUMS. Bundle berisi commit implementasi beserta dokumen review, tidak membawa seluruh riwayat lama atau database pribadi. Bundle membutuhkan commit dasar main yang disebut pada bagian 1. Patch dan bundle adalah pilihan alternatif, jangan menerapkan keduanya pada branch yang sama.

Build APK: DITAHAN. Kotlin dan tes lokal lulus, tetapi review pengguna, dependensi produksi dan pengujian perangkat masih diperlukan. Tidak ada APK, merge, aktivasi pengiriman atau perubahan jadwal yang dilakukan dalam pekerjaan ini. PR draft Android #2 dan backend #2 sudah dibuat tanpa merge. Android: https://github.com/ilhamramdhani763-stack/automate-jobs-android/pull/2 . Backend: https://github.com/ilhamramdhani763-stack/automate-jobs-backend/pull/2 . Persetujuan produksi tetap diperlukan sebelum tindakan produksi apa pun.
