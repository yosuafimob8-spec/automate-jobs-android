# KarirMate — hasil integrasi Termux, dashboard, dan ekspor PDF

Laporan ini mencakup perubahan terhadap `main` pada `b0c7209a51d56a523d82047d73cb0b78c9710355`, termasuk perbaikan CV/Psikotes sebelumnya. Branch: `fix/karirmate-comprehensive-repair`. Source Termux berasal dari arsip pengguna dan diintegrasikan dalam draft PR repository backend terpisah. Tidak ada build APK, merge atau tindakan produksi.

## Hasil implementasi

| Area | Bug/kebutuhan dan akar masalah | Perbaikan yang tersedia |
| --- | --- | --- |
| CV Builder — navigasi | State nomor halaman dan tujuan kembali tetap berbeda antara toolbar/Android Back; tombol kembali ganda. | Riwayat SavedStateHandle, satu panah header dan handler Back yang sama. Dashboard bottom nav menuju Home. |
| Formulir dan warna | Draf baru hanya berada di memori, label menumpuk, komponen mewarisi teks gelap/terang yang salah. | Autosave sejak awal, pemulihan draf aktif, AppInput reusable, tema kanvas sesuai layar, kontras warna diuji. |
| Foto CV | Referensi dan renderer tidak mendukung foto. | Picker galeri, salinan privat persisten, normalisasi EXIF, lingkaran, ganti/hapus, foto pada template yang mendukung; ATS tanpa foto. |
| Template/preview | Puluhan ID ilustratif, PRO dari nomor, thumbnail/preview/PDF berbeda. | Katalog kurasi 14 layout dalam 7 kategori, ID lama tetap dibaca, satu layout A4 dan fungsi dekorasi; preview scroll/zoom dengan data pengguna, contoh hanya bila kosong. |
| PDF CV dan surat | Exporter awal berbeda dari preview; Android PdfDocument tidak dapat diuji dalam Robolectric. | PDFBox dengan font Unicode berlisensi yang sama pada layout/preview; teks dapat dipilih, dekorasi/foto sama, margin dan multipage. PDF sebenarnya diuji dan dibuka dengan parser independen; ekspor tidak lagi bergantung native PdfDocument. |
| CV Analyst | PDF pilihan hanya dicatat namanya; checker menganalisis draf lain. | Ekstraksi teks PDF pilihan, skor struktur lokal 0–100, kelebihan/kekurangan/rekomendasi serta riwayat; gagal ekstraksi tidak menghasilkan skor. OCR belum ada. |
| Surat lamaran | Tombol hanya mencetak teks Inggris, tidak memiliki editor/simpan/ekspor. | Pilihan standar/pengalaman/kemampuan, fakta CV aktif/PDF terpilih, perusahaan/posisi, editor tersimpan, preview dan PDF. Pilihan AI jujur belum dikonfigurasi. |
| Psikotes | PRO hardcoded, jawaban paralel dengan revisi lama, submit gagal mengunci UI, skor berbobot dibulatkan salah. | PRO dari akun, pilihan lokal persisten dan sinkronisasi serial, retry setelah kegagalan, sesi Android 10–20, skor berbobot 1–100, kepribadian tetap interpretasi, riwayat dan statistik PRO. Service/policy juga tersedia di repo backend. |
| Auto Apply Android | Consent tidak tersambung ke server, OAuth/upload CV tidak mempunyai alur, report tidak berisi items. | Endpoint OAuth dibuka lewat browser Google tervalidasi, upload PDF mentah ke /cv, status setelah kembali ke aplikasi, consent/batas lima, jeda dan riwayat terkonfirmasi ID Gmail. Revokasi server mengalahkan cache lokal. |
| Dashboard | Empat kartu lama diminta diganti. | Kartu Cari Lowongan diganti Pelatihan Kerja; Riwayat diganti Kalkulator Gaji; Surat Izin diganti Surat Lamaran & Resign; Profil diganti Dokumen Karier. Bottom nav asli tetap sama. Profil/Cari/Riwayat tetap dapat diakses; Surat Izin lama tersedia dari Dokumen Karier. |
| Pelatihan kerja | Fitur baru harus benar-benar dapat dipakai. | Dua modul belajar mandiri, materi dan kuis dengan kunci nyata, penilaian minimal 80, riwayat lokal, sertifikat PDF. Label tegas bahwa sertifikat internal, bukan BNSP/akreditasi atau jaminan kerja. |
| Kalkulator gaji | Perlu kalkulasi nyata dengan asumsi terbuka. | Lembur 1/173 dan faktor hari kerja/istirahat; kontribusi pekerja kesehatan/JHT/JP; THR proporsional; estimasi PPh 21 tahunan berdasarkan tarif HPP, biaya jabatan, PTKP dan kontribusi pensiun. Tidak mengklaim angka rata-rata sebagai potongan TER bulanan. Dasar JP setelah plafon aktual dimasukkan pengguna, tidak ditebak. |
| Surat resign | Fitur baru tanpa fakta fiktif. | Nama/perusahaan/jabatan/alasan/tanggal pengguna, validasi tanggal, draf editable tersimpan otomatis, preview A4 dan PDF. Tanggal akhir merupakan usulan, bukan klaim pemenuhan masa pemberitahuan hukum. |
| Dokumen karier | Tombol pengelolaan harus memproses file sebenarnya. | Impor PDF privat, gabung, ekstrak/susun halaman, putar 90°, simpan melalui picker Android dan bagikan via FileProvider. Operasi membuat file baru dan mempertahankan sumber. Tidak mengklaim seluruh fitur iLovePDF, OCR, atau kompresi yang belum dibuat. |

## Pengujian akhir

| Pemeriksaan | Status | Bukti/batas |
| --- | --- | --- |
| compileDebugKotlin | PASS | Seluruh source aplikasi berhasil dikompilasi. |
| testDebugUnitTest | PASS | **74 tes**, 0 gagal, 0 dilewati; termasuk alur UI CV, Back, surat, Psikotes, resign dan penyelesaian pelatihan. |
| Tes kontrak backend dalam repo Android | PASS | **13 tes**; tidak menggunakan jaringan Gmail. |
| Tes repo backend Termux | PASS | **28 tes**; database sementara, transport Gmail mock; kontrak HTTP memakai server loopback dan identitas fixture. |
| lintDebug | PASS dengan warning | 0 error, **66 warning**. Bukan sertifikasi keamanan atau kesiapan rilis. |
| compileDebugAndroidTestKotlin | PASS | Source tes perangkat dapat dikompilasi; tidak menjalankan APK. |
| PDF sebenarnya | PASS lokal | CV A4 multipage, teks Indonesia/Unicode, isi terakhir, dokumen gabungan/ekstraksi/rotasi, surat dan sertifikat diperiksa lewat PDFBox. CV empat halaman juga dibuka dan diekstrak dengan PyMuPDF independen. |
| Android viewer, picker galeri/provider nyata dan share target | NOT TESTED di perangkat | Handler/penyimpanan diuji lokal; belum ada emulator/perangkat yang terhubung ke lingkungan ini. |
| OAuth akun nyata, refresh token, scope Gmail, pengiriman dan cron | NOT TESTED | Tidak dipanggil/diubah; perlu worker dan dependensi produksi. |
| Login/registrasi/langganan dengan akun produksi | NOT TESTED | Policy dan penolakan saat dependensi hilang diuji; tidak memakai akun nyata. |

Log: `docs/validation/termux-integration-gradle.txt`, `docs/validation/termux-integration-results.json`; log backend ada pada draft PR backend. Tidak ada assemble/build APK atau GitHub Actions yang dijalankan. Wrapper memakai SDK 35, build tools 36, JVM daemon 25 dan JVM tes 21. Proxy/CA/cache sandbox dikonfigurasi lewat init script lingkungan, bukan source produk.

Perintah lokal:

```sh
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin
python -B -m unittest discover -s backend-contract/tests -v
```

Kegagalan yang ditemukan selama iterasi telah diperbaiki/diperjelas: resource halaman impor PDF dipertahankan; parser teks pada halaman diputar menambah pemisah baris sehingga tes juga memeriksa byte content stream; fixture Retrofit diubah menjadi byte PDF seperti kode produksi. Foto PDF diverifikasi lewat stream DeviceRGB asli serta renderer PyMuPDF independen; decoder gambar PDFBox Android pada runtime uji menukar kanal merah/biru saat getImage, sehingga tidak dipakai sebagai satu-satunya oracle. Tidak ada tes gagal yang disembunyikan lewat skip.

## Batas yang masih berlaku

1. Source worker OAuth/Gmail, modul auth/langganan, bank Psikotes dan beberapa modul katalog belum disertakan. Alur lokal menolak aktivasi jika identitas/langganan tidak dapat diverifikasi. Versi aplikasi/server produksi tidak otomatis berubah karena PR dibuat.
2. Cron Termux tetap tanpa --send sesuai informasi pengguna. Sender produksi memerlukan izin operator terpisah; tidak diaktifkan dalam pekerjaan ini. Worker harus menyatakan mendukung narasi spontan sebelum jenis tersebut dapat dikirim.
3. Kandidat tanpa lokasi/sumber atau bukti lowongan tidak ditingkatkan menjadi lowongan terverifikasi. Bisa ada kurang dari lima target yang memenuhi syarat; alamat, posisi dan tanggal tidak dikarang.
4. AI surat belum diintegrasikan; tiga generator lokal tetap berfungsi. Analyst melakukan pemeriksaan struktur berbasis aturan, bukan prediksi kelulusan perekrut dan bukan OCR.
5. Kalkulator adalah estimasi untuk penghasilan bulanan tetap dengan THR serta lembur input satu kali. Rekonsiliasi PPh 21 TER, komponen pajak tambahan, status pajak khusus, dan plafon JP aktual tetap memerlukan data payroll yang tepat. Rujukan model: tarif UU HPP; PMK 168/2023 untuk membedakan TER; PP 35/2021 lembur; Permenaker 6/2016 THR.
6. Sertifikat pelatihan hanya bukti penyelesaian dua modul internal. Tidak mengklaim pelatihan resmi, verifikasi identitas peserta, atau sertifikasi kompetensi eksternal.
7. Dokumen PDF dibatasi 15 MB per impor, 30 MB/100 halaman per gabungan dan 2–10 sumber. PDF terenkripsi ditolak. Gabung/susun/putar mempertahankan sumber; OCR, redaksi aman, tanda tangan digital, kompresi lanjutan dan konversi Office belum tersedia.
8. Pembukaan file pada viewer Android nyata, OAuth browser kembali ke aplikasi, pemilihan foto provider tertentu serta performa perangkat RAM kecil masih perlu diuji sebelum rilis.

## Audit regresi

Bottom nav tetap `dashboard`, `cari_lowongan`, `kirim_lamaran`, `riwayat`, `profil` dan diuji. Penyimpanan CV lama tetap terbaca; field tambahan memiliki default. ID template lama dipetakan tanpa menghapus draf. Data profil, riwayat, dokumen kerja dan CV yang sudah ada tidak dimigrasikan secara destruktif. Fitur Surat Izin lama tetap memakai route dan penyimpanannya.

Risiko yang perlu ditinjau: ukuran aplikasi bertambah akibat font Unicode/PDFBox; warning lint dependensi kriptografi dan cleartext debug; perbedaan tata letak ID template lama; performa dokumen besar; source backend yang belum lengkap; pencatatan status pengiriman lama yang memerlukan pemeriksaan manual. File sertifikat/surat adalah dokumen pengguna, bukan bukti resmi dari pihak luar.

## Source dan review

Dua draft PR ditujukan ke main masing-masing repository, tanpa merge. Workflow APK repository Android hanya push main/master atau workflow_dispatch; push branch perbaikan tidak memicunya. Render backend tetap autoDeploy false dan health-only. Tidak ada token, kredensial atau database pribadi yang dimasukkan.

Dokumen audit sebelumnya dipertahankan sebagai catatan historis dan diberi tautan ke laporan ini. Status ekspor PDF lokal dalam laporan ini menggantikan batas native PdfDocument pada audit awal.

## Daftar file terhadap basis main

- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/example/automatesmartbot/data/cv/CvPdfDeviceTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/fonts/DejaVuSans-Bold.ttf`
- `app/src/main/assets/fonts/DejaVuSans.ttf`
- `app/src/main/assets/fonts/LICENSE.txt`
- `app/src/main/java/com/example/automatesmartbot/MainActivity.kt`
- `app/src/main/java/com/example/automatesmartbot/data/autoapply/AutoApplyConfirmation.kt`
- `app/src/main/java/com/example/automatesmartbot/data/career/CareerLetters.kt`
- `app/src/main/java/com/example/automatesmartbot/data/career/CareerPdfStore.kt`
- `app/src/main/java/com/example/automatesmartbot/data/career/PayrollCalculator.kt`
- `app/src/main/java/com/example/automatesmartbot/data/career/TrainingCourses.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvAtsChecker.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvCoverLetter.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvDocument.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvDraftStore.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvExporter.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvLetterStore.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvPhotoStore.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvRenderer.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvTemplates.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/PdfCvAnalyzer.kt`
- `app/src/main/java/com/example/automatesmartbot/data/model/AuthModels.kt`
- `app/src/main/java/com/example/automatesmartbot/data/network/ApiService.kt`
- `app/src/main/java/com/example/automatesmartbot/data/psychotest/PsychotestPolicy.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/components/AppInput.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/components/CvDocumentPreview.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/components/CvSection.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/components/PdfExportButton.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/navigation/CvNavigation.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/navigation/Screen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/CareerDocumentsScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/CareerToolsScreens.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/CariLowonganScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/CvBuilderScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/CvEntriesEditor.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/KirimLamaranScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/LatihanPsikotesScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/MainDashboardScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/ProfilScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/theme/Color.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/theme/Theme.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/viewmodel/CvBuilderViewModel.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/viewmodel/PsychotestViewModel.kt`
- `app/src/main/res/xml/career_file_paths.xml`
- `app/src/test/java/com/example/automatesmartbot/data/autoapply/AutoApplyConfirmationTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/career/CareerPdfStoreTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/career/PayrollAndLettersTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/cv/CvFlowTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/model/MembershipTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/network/AutoApplyApiContractTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/psychotest/PsychotestPolicyTest.kt`
- `app/src/test/java/com/example/automatesmartbot/ui/screens/FeatureUiTest.kt`
- `app/src/test/java/com/example/automatesmartbot/ui/theme/ThemeContrastTest.kt`
- `app/src/test/java/com/example/automatesmartbot/ui/viewmodel/PsychotestViewModelTest.kt`
- `backend-contract/auto_apply_safety.py`
- `backend-contract/psychotest_api.py`
- `backend-contract/psychotest_policy.py`
- `backend-contract/psychotest_service.py`
- `backend-contract/tests/test_psychotest_service.py`
- `backend-contract/tests/test_safety.py`
- `docs/KARIRMATE_REPAIR_REPORT.md`
- `docs/KARIRMATE_TERMUX_AND_CAREER_REPORT.md`
- `docs/validation/gradle-local.txt`
- `docs/validation/python-local.txt`
- `docs/validation/results.json`
- `docs/validation/termux-integration-gradle.txt`
- `docs/validation/termux-integration-results.json`
- `gradlew`
