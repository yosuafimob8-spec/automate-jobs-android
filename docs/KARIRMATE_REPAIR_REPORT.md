# Laporan perbaikan KarirMate

Basis audit: `main` pada `b0c7209a51d56a523d82047d73cb0b78c9710355`.
Branch lokal: `fix/karirmate-comprehensive-repair`.

Status: implementasi lokal tersedia untuk ditinjau, **belum siap dinyatakan seluruhnya selesai**. Tidak ada push, merge, GitHub Actions, atau pengiriman email sungguhan. Tidak ada kredensial Gmail yang dipakai selama pengujian.

## Bug, penyebab, dan perbaikan

| Area | Temuan dan akar masalah | Perbaikan |
| --- | --- | --- |
| Navigasi CV | Halaman memakai angka dan tujuan kembali tetap; toolbar shell langsung keluar sementara Back Android memakai logika lain. Tombol kembali bertumpuk di konten. | Riwayat halaman di SavedStateHandle; header dan Back Android memanggil pop yang sama; hanya header memiliki panah. Dashboard bottom navigation langsung menuju Home dan mereset riwayat CV. |
| Draf | updateContent hanya menulis jika dokumen sudah disimpan sebelumnya; CV baru hilang ketika ViewModel dibuat ulang. | Dokumen dibuat secara persisten sejak awal, setiap perubahan disimpan, dokumen aktif dipulihkan, ID memakai UUID. Draf lama tetap dibaca; field baru punya default. |
| Warna/formulir | Semua label Informasi Pribadi diletakkan sebelum seluruh input; beberapa kanvas putih mewarisi warna komponen tema gelap. | AppInput menempatkan label di atas inputnya, menggunakan warna MaterialTheme. Tema kanvas tetap untuk layar putih/dark auth, pasangan warna komponen profil/CV diperbaiki, tombol Psikotes oranye dengan teks putih yang diuji kontrasnya. |
| Foto | Model, penyimpanan, dan renderer tidak mempunyai referensi foto. | Picker Android OpenDocument, validasi dan batas ukuran, downsampling/rotasi EXIF, salinan privat persisten, tampilan lingkaran, ganti/hapus, foto pada renderer template yang mendukungnya. ATS tanpa foto. |
| Template | Puluhan ID variasi ilustrasi, PRO dikunci dari nomor ID, preview berbeda dari thumbnail dan PDF. | Katalog kurasi 14 layout dalam 7 kategori; satu model layout dan fungsi gambar dipakai thumbnail, preview, dan PDF. ID lama dipetakan tanpa menghapus CV lama. Semua desain katalog tersedia tanpa kunci PRO palsu. |
| Preview/PDF | Preview Compose dan exporter teks polos terpisah; preview tidak A4/zoom dan tidak memuat semua field. | Layout dalam koordinat A4 595×842, pembungkusan teks dan paginasi, semua bagian/data CV, foto, preview per halaman dengan scroll/pinch/zoom. Data contoh hanya untuk preview CV tanpa isi; ekspor tidak memasukkan data contoh. Snapshot ekspor bertahan saat picker/rotasi. Ekspor DOCX lama tetap tersedia. |
| Analyst | Picker PDF hanya menyimpan nama, checker tetap menganalisis draf lain. | PDFBox mengekstrak teks file pilihan; analisis aturan lokal 0–100, kelebihan/kekurangan/rekomendasi, riwayat nyata. PDF rusak, terenkripsi, terlalu besar, atau hasil pindai tanpa teks menghasilkan error tanpa skor. Tidak memerlukan deskripsi lowongan. |
| Checker draf | Mengklaim layout/font PDF lolos tanpa memeriksa PDF; pembagian integer pada rasio pencapaian; riwayat pengalaman diurutkan sebelum diperiksa. | Klaim geometri/font palsu dihapus; rasio menggunakan pecahan dan pemeriksaan urutan memakai urutan asli. |
| Surat lamaran | Tombol hanya menampilkan teks Inggris di panel tanpa alur jenis/editor/simpan/ekspor. | Empat pilihan, perusahaan/posisi tujuan, generator Indonesia untuk tiga jenis, memakai fakta CV aktif/PDF terpilih, editor tersimpan, preview A4, ekspor PDF. AI menampilkan status belum dikonfigurasi dan tidak menghasilkan surat seolah-olah memakai AI. |
| Isian lain CV | Proyek/pendidikan/sertifikasi hanya memiliki sebagian field; beberapa menu sekadar pesan belum didukung. | Editor lengkap untuk field model, referensi/bagian tambahan persisten, urutan bagian; judul bagian tambahan dapat diedit. |
| Psikotes jawaban | Request jawaban paralel memakai revisi lama; pilihan baru terlihat setelah API berhasil. Submit gagal mengunci tombol lewat boolean lokal. | Pilihan langsung tersimpan di SavedStateHandle, sinkronisasi serial dengan mutex dan batch, refresh revisi sekali pada konflik, retry setelah koneksi gagal, submit menunggu sinkronisasi dan kembali dapat dicoba jika gagal. |
| Psikotes akses/jumlah | PRO hardcoded false; mode dasar ditolak jika konfigurasi lama tidak tepat 10–20. | PRO dari status akun yang sudah digunakan aplikasi; backend membedakan akses basic/PRO. Android meminta sesi latihan 10–20; backend sampling nyata dengan pemeriksaan kecukupan bank, tanpa soal duplikat/palsu. Kontrak sesi penuh klien lama tetap dipertahankan. |
| Skor/hasil | Menggunakan integer untuk skor berbobot, rentang mulai 0 dan tes kepribadian dipaksa menjadi skor. | Normalisasi skor berbobot 1–100, evaluasi/motivasi menurut hasil. Kepribadian tetap interpretasi, tidak diberi nilai benar/salah fiktif. Riwayat server ditampilkan dan statistik PRO dibaca terpisah. Timer memakai elapsedRealtime. |
| Langganan | Label membership PRO dapat mengalahkan is_pro=false yang otoritatif. | Boolean is_pro dari server didahulukan; label hanya fallback jika boolean belum tersedia. |
| Auto Apply UI | Gaya email hanya state lokal tanpa dipakai API, narasi pencarian random, izin diulang dan nilai null dapat dianggap consent/message ID. | Kontrol gaya tanpa efek diganti alur status dan persetujuan nyata; request enable menyertakan consent dan batas 5; consent akun dicatat setelah server mengonfirmasi aktif. Gmail harus terhubung. Riwayat hanya disebut terkirim jika ada ID Gmail; null bukan konfirmasi. Kegagalan status/aktivasi tidak diklaim berhasil. |
| Auto Apply server | OAuth, scheduler, sender, database perusahaan, dan endpoint 8787 tidak ada dalam repository. | Modul terpisah auto_apply_safety disiapkan: pemilihan kandidat berbasis kontak sumber, prioritas lowongan aktif terverifikasi yang sesuai posisi/lokasi, fallback spontan berlabel jujur, ledger SQLite atomik, anti-duplikat, batas 5/hari Asia/Jakarta termasuk hasil belum pasti, status sent hanya sesudah ID dari sender Gmail. **Modul belum dihubungkan ke server produksi yang tidak tersedia.** |
| Build | gradlew tidak executable; URL psikotes release selalu kosong. | Izin executable diperbaiki; property releasePsychotestBaseUrl opsional dengan HTTPS, tanpa mengarang alamat server. |

## Pengujian

Perintah lokal standar:

```sh
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin
python -B -m unittest discover -s backend-contract/tests -v
```

Lingkungan ini memakai Android SDK 35, build tools 36, Gradle wrapper 9.6, JVM daemon 25, JVM tes 21. Cache dan konfigurasi Android ditempatkan di `/workspace` karena home read-only. Proxy/CA dan direktori cache Robolectric diberikan lewat init script lingkungan `/workspace/karirmate-testing.init.gradle`, bukan disisipkan dalam source aplikasi. Tidak ada workflow GitHub yang dijalankan.

Hasil akhir pemeriksaan lokal:

| Pemeriksaan | Status | Bukti/batas |
| --- | --- | --- |
| compileDebugKotlin | PASS | Source aplikasi berhasil dikompilasi. |
| testDebugUnitTest | PASS | **58 tes**, 0 gagal, 0 dilewati; termasuk 3 tes UI Robolectric. |
| Backend Python | PASS | **13 tes**, tanpa jaringan/sender nyata. |
| lintDebug | PASS dengan warning | 0 error, **59 warning**; termasuk lint dependensi kriptografi PDFBox dan HTTP pada konfigurasi debug. |
| compileDebugAndroidTestKotlin | PASS | Tes perangkat berhasil dikompilasi, bukan dieksekusi. |
| connectedDebugAndroidTest / ekspor PDF native | NOT TESTED | Tidak tersedia emulator/perangkat; ADB di sandbox juga tertahan direktori home read-only. |
| Gmail OAuth, token refresh, pengiriman dan scheduler produksi | NOT TESTED | Source backend dan koneksi uji tidak tersedia; tidak mengirim email. |
| Login/registrasi/profil/API akun nyata | NOT TESTED | Policy/regresi unit lulus; tidak menjalankan request akun nyata. |
| Bank dan backend Psikotes lengkap | NOT TESTED | Modul bank/identity bridge produksi tidak ada; service diuji memakai fixture khusus tes. |
| git diff --check | PASS | Tidak ada whitespace error. |

Log: [Gradle](validation/gradle-local.txt), [Python](validation/python-local.txt), [ringkasan mesin](validation/results.json).

Gambar thumbnail yang dihasilkan tes tersedia pada `app/build/reports/cv-previews/` (artefak build lokal, tidak masuk commit). Gambar Profesional Sidebar, Lulusan Akademik, dan Kreatif Portofolio telah diperiksa secara visual. Tes perangkat `CvPdfDeviceTest` memerlukan emulator/perangkat Android dan tidak boleh dihitung sebagai tes yang telah dijalankan hanya karena berhasil dikompilasi.

Cakupan tes lokal baru:

- Navigasi header/Back Android bertahap serta formulir tetap terisi (tes UI dan ViewModel).
- Round-trip draf, kompatibilitas JSON lama, pemilihan/salinan/hapus foto dan file invalid.
- Seluruh kategori/layout template, thumbnail nyata, batas koordinat dan paginasi CV panjang, foto hanya pada template yang mendukungnya.
- PDF teks nyata sebagai input Analyst, ekstraksi gagal tanpa skor, surat berdasarkan teks yang dibaca.
- Tiga jenis surat Indonesia, penolakan AI yang belum tersedia, fakta CV kosong tidak dikarang, penyimpanan edit dan alur UI sampai preview/ekspor.
- Navigasi soal dan pilihan tetap terpilih, revisi serial, koneksi putus dan pemulihan jawaban, submit dapat dicoba kembali, sesi kosong.
- Skor berbobot, batas 1–100, status PRO otoritatif, kontras teks tema dan tombol.
- Persetujuan dan ID Gmail null tidak dianggap berhasil; anti-duplikat persisten, batas lima dengan permintaan paralel, hari berikutnya, timeout tidak dikirim ulang.
- Service Psikotes asli diuji dengan bank sintetis khusus tes dan SQLite sementara. Fixture ini **bukan bank produksi** dan tidak menggantikan modul yang hilang.

## Batas penyelesaian dan risiko regresi

1. **Ekspor PDF native belum tervalidasi di perangkat.** Robolectric tidak menyediakan implementasi native PdfDocument/PdfRenderer. Percobaan ekspor dalam JVM gagal karena native document tidak dibuat; tes aktual dipisahkan ke androidTest, bukan diberi hasil PASS. Layout/draw/preview/paginasi dan ekstraksi PDF teks diuji lokal. Jalankan `./gradlew connectedDebugAndroidTest` pada perangkat sebelum rilis untuk ekspor CV, surat, foto, ukuran A4, pembukaan PDF, dan multipage.
2. **Gmail OAuth/refresh token/scheduler/pengiriman nyata belum diperbaiki end-to-end** karena source server tidak ada. Frontend tetap menggunakan endpoint yang sudah ada; backend harus menerima consent/batas, memasang gate, memverifikasi langganan dan memakai adapter Gmail asli. Tes sender memakai callback tes tanpa jaringan. Jangan menganggap batas lima sudah berlaku di server produksi sebelum gate dipasang.
3. **Backend Psikotes tidak mandiri dalam repository:** psychotest_bank, psychotest_skb, search_identity dan modul pencarian yang diimpor backend-contract tidak disertakan. Kontrak/perbaikan service dapat ditinjau dan dites dengan fixture, tetapi bank/jumlah soal serta langganan akun nyata masih memerlukan integrasi server lengkap. Identity bridge harus mengembalikan is_pro/membership yang benar dari auth/me; jika tidak tersedia akses PRO ditolak. URL release perlu dikonfigurasi sesudah server HTTPS tersedia.
4. **AI surat belum tersedia** dan secara sengaja ditampilkan demikian. Tiga generator aturan lokal berfungsi, tetapi tidak mengklaim melakukan inferensi AI.
5. **Analyst adalah analisis struktur berbasis teks**, tidak menilai kualitas kandidat secara objektif dan tidak melakukan OCR. CV dengan judul bagian yang tidak dikenali dapat memperoleh rekomendasi pemeriksaan manual. Ekstraksi susunan kompleks dan font khusus perlu diuji dengan contoh PDF pengguna.
6. **Migrasi template lama:** ID dan isi disimpan, namun desain lama yang ilustratif dipetakan ke desain kurasi terdekat. Perlu peninjauan visual dengan CV pengguna panjang. Template akademik/keterampilan/portofolio mendahulukan bagian utamanya; urutan lain mengikuti pengaturan.
7. **Login/registrasi/API nyata belum diuji ulang dengan akun live.** Unit policy sesi/navigasi/profil yang sudah ada dijalankan; request login, register, save profile dan endpoint existing tidak diubah. Tema auth disesuaikan untuk kanvas gelap. Surat Izin & Dokumen Kerja hanya memperoleh tema kanvas terang pada shell; isi, ekspor dan API-nya tidak diubah atau diklaim lolos pengujian perangkat.
8. PDFBox menambah ukuran dependensi dan dependensi kriptografi transitif. Lint masih melaporkan warning pihak ketiga serta konfigurasi HTTP debug yang sudah ada; bukan hasil audit keamanan menyeluruh. Release network-security tetap menolak cleartext.
9. Penyimpanan CV tetap lokal sesuai arsitektur yang ada; ini bukan implementasi sinkronisasi akun/cloud baru. File PDF yang diekspor dapat dipilih lewat fitur unggah CV yang sudah ada.

## Review lokal

Perubahan belum dipublikasikan. Patch dan bundle Git lokal disiapkan setelah commit, sehingga dapat ditinjau tanpa push atau memicu GitHub Actions. PR belum dibuat karena memerlukan publikasi branch yang belum disetujui.

## File yang diubah/ditambahkan

- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/com/example/automatesmartbot/data/cv/CvPdfDeviceTest.kt`
- `app/src/main/java/com/example/automatesmartbot/data/autoapply/AutoApplyConfirmation.kt`
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
- `app/src/main/java/com/example/automatesmartbot/ui/navigation/CvNavigation.kt`
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
- `app/src/test/java/com/example/automatesmartbot/data/autoapply/AutoApplyConfirmationTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/cv/CvFlowTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/model/MembershipTest.kt`
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
- `docs/validation/gradle-local.txt`
- `docs/validation/python-local.txt`
- `docs/validation/results.json`
- `gradlew`
