# KarirMate STEP 3 → FINAL: hasil lokal untuk review

Catatan publikasi: laporan ini mencatat keadaan validasi lokal sebelum commit. Pengguna kemudian mengizinkan commit/push untuk review; status gerbang terbaru ada di checkpoint. Build APK belum diizinkan.

Branch `integration/latest-base-all-fixes-20261102`. Worktree `/workspace/karirmate-v78-ui-fixes`. Dasar terbaru diperiksa melalui fetch dan fast-forward ke `58aaf6d188e2ac6c13233e755171c7ec9d4f7972`; perbaikan sebelumnya tetap menjadi ancestor. Tidak membuat branch atau commit baru, mengubah main, menggabungkan pekerjaan bank soal, push, atau membangun APK.

**Hasil akhir: 142 test Android/Compose + 23 test backend lulus; nol gagal. Kompilasi Kotlin debug dan Termux lulus.**

## Status

| STEP | Hasil lokal | Batas verifikasi |
|---|---|---|
| 3 | Katalog 75/27 FREE/48 PRO; struktur tambahan; preview/editor; otorisasi dan snapshot ekspor; PDF/DOCX; draf dan isolasi akun diuji | Draf lama tanpa pemilik tidak boleh otomatis diklaim akun baru. Word/LibreOffice dan Android nyata belum diuji. |
| 4 | Empat paket simulasi, konfirmasi, aktivasi atomik/idempoten, perpanjangan, restart, kedaluwarsa, tiga halaman | Tidak ada transaksi atau hak PRO server dari demo. |
| 5 | Seluruh unit/Compose test, kompilasi debug/Termux, pengujian kontrak backend dan pemisahan akun | Backend Termux nyata dan komponen identitas produksi tidak tersedia di Cloud; adapter identitas/bank terisolasi dipakai dalam uji kontrak. |
| 6 | Diff, scope, artefak, credential-pattern review, checkpoint, bukti pengujian, dan risiko disiapkan | Siap review dan tahap build pengujian setelah izin; bukan klaim siap rilis produksi atau 100% terverifikasi. |

## STEP 3

Katalog tetap deterministik: 75 ID unik, 27 FREE dan 48 PRO. Empat belas template awal tetap tersedia dan FREE. Alias `standard`, `student`, `combination`, `senior` tetap dapat dibuka; ID tidak dikenal tidak diberi akses. Daftar ID kanonis mencakup seluruh katalog.

Sebanyak 14 desain lama dipertahankan; 61 tambahan memakai kombinasi struktural dari lima susunan header, empat susunan kolom, dan empat prioritas bagian. Ini bukan klaim 75 keluarga layout yang digambar terpisah. Keunikan diuji dari teks/koordinat/ukuran/hierarki tanpa warna atau ID sebagai fingerprint, lalu lembar kontak 75 template ditinjau. Seluruh layout diuji dengan data panjang: batas A4, tabrakan teks, keberadaan isi terakhir, dan foto pada template yang mendukungnya. [Lembar kontak](verification/cv-75-contact-sheet.png).

Katalog tetap 12 kartu per halaman; layout dan rasterisasi thumbnail berjalan di background dispatcher. Thumbnail memakai data draf pengguna bila tersedia. Preview dan ekspor menggunakan geometri CvPage yang sama. PDF mempertahankan teks Unicode yang dapat dipilih; DOCX memakai artwork halaman dan text frame yang dapat diedit dengan posisi dari layout yang sama. Paket XML, isi teks, jumlah halaman, gambar, dan page break DOCX diperiksa. [Contoh DOCX sintetis](verification/sample-layout.docx). Rendering Word/LibreOffice nyata belum dijalankan.

Hak template tidak ditentukan oleh `isPro` dari Compose. Sumber produksi memeriksa `/auth/me` yang terautentikasi, sesi yang sama, status PRO dan expiry eksplisit. Hasil tanpa expiry mempunyai lease singkat; UI memeriksa ulang berkala. Setiap ekspor PRO memeriksa backend lagi. Template, data, dan identitas sesi ditangkap saat memulai ekspor, sehingga callback picker tidak memakai template lain yang kebetulan aktif kemudian. Rendering dan pemeriksaan akhir selesai SEBELUM membuka/truncate tujuan: test kedaluwarsa di tengah ekspor memastikan dokumen lama tidak berubah.

Draf memakai AtomicFile. Kegagalan simpan tidak mengklaim sukses atau menimpa versi lama. Draf PRO tidak dihapus saat hak berakhir dan muncul kembali setelah hak server aktif. Pemilihan/edit/ekspor tidak dapat diaktifkan hanya dengan mengubah flag UI.

Penyimpanan akun diikat ke identitas pemilik yang diverifikasi backend, bukan sekadar token/claim JWT yang dibaca lokal. Pengikatan disimpan menggunakan hash; token opaque baru untuk pemilik yang sama tetap membuka workspace yang sama. Akun berbeda memperoleh workspace berbeda. Pada koneksi pertama, penulisan menunggu verifikasi pemilik; setelah binding tersimpan, draf FREE dapat dibuka kembali tanpa menganggap demo sebagai otorisasi. Data CV, foto, surat, analisis, dan demo dipisahkan menurut workspace.

**Batas migrasi penting:** draf perangkat dari versi lama tidak menyimpan identitas pemilik yang dapat dipercaya. File aslinya tetap utuh dan UI memberikan pemberitahuan; file tersebut tidak otomatis dipindah ke akun yang sedang login. Penetapan pemilik/migrasi data lama ini memerlukan bukti/kebijakan kepemilikan. Kompatibilitas ID lama lulus; migrasi pemilik data lama belum dinyatakan selesai. Jangan merilis migrasi diam-diam yang berisiko memperlihatkan CV akun lain.

## STEP 4

| Paket | Harga simulasi | Masa aktif |
|---|---|---|
| Harian | Rp5.000 | 1 hari |
| Mingguan | Rp25.000 | 7 hari |
| Bulanan | Rp79.000 | 30 hari |
| Tahunan | Rp499.000 | 365 hari |

Upgrade → pilih paket → konfirmasi checkbox → aktivasi demo → Kelola Langganan → perpanjang. Perpanjangan menambah durasi dari `max(now, expiry)`; batas tepat expiry sudah nonaktif. Identitas paket, waktu aktivasi, expiry, dan ID permintaan disimpan atomik. Replay request, klik berulang, request bersamaan, restart, overflow, ID paket invalid, dan kegagalan tulis diuji. UI memperbarui status dengan clock dan menampilkan tanggal dalam zona perangkat.

Tidak ada gateway, transaksi uang, pemotongan saldo, perubahan token, atau peningkatan hak server. Demo tidak memberi hak template PRO produksi. Hak PRO dalam pengujian menggunakan authority fixture di source test, bukan switch/bypass produksi. Demo juga memakai binding akun terverifikasi agar renewal token tidak menghilangkan status atau memindahkannya ke akun lain.

## STEP 5: bukti dan keamanan

Perintah Android yang dipakai setelah dry-run memeriksa task graph:

```text
./gradlew :app:testDebugUnitTest :app:compileTermuxKotlin --offline
python -m unittest discover -s backend-contract/tests -v
```

Runner Cloud memakai SDK/Gradle cache lokal dan init script Robolectric yang dapat ditulis. Sandbox memerlukan akses socket untuk Gradle daemon dan event loop test; percobaan tanpa akses ini tidak menjalankan validasi dengan benar dan diulang dalam lingkungan yang sesuai. Tidak ada dependensi baru atau task APK.

- Hasil akhir dan semua nama test dicatat di [results.json](verification/results.json).
- [Log Android dan kompilasi](verification/android-validation.txt), [log backend](verification/backend-validation.txt), [task graph tanpa APK](verification/gradle-taskgraph.txt).
- Regresi mencakup login/error, navigasi bawah, Dashboard/CV, draf/letter/PDF, tema, simulasi gaji, training/certificate, audio, quota/API Auto Apply, dan fitur psikotes/interview yang sudah mempunyai test.
- API backend yang diuji menolak klaim `is_pro`, `membership`, atau `pro_demo` dari body client; sumber hak tetap identitas server. Parameter account dari client tidak mengganti identitas server. Session akun lain ditolak.
- Review pola token/private key pada file berubah tidak menemukan credential nyata. Tidak mengubah API/backend produksi atau workflow.
- Tidak ada APK/AAB di worktree perbaikan. Tidak ada assemble/install/packaging APK pada task graph.
- Test awal menemukan signature CV screen pada source remote yang merusak trailing-lambda caller; signature diperbaiki. Kesalahan quote XML DOCX ditemukan dan diperbaiki. Tidak ada kegagalan test yang diabaikan.

## Regresi audio/visual yang dipertahankan

Perbaikan warna semua status tombol Materi Audio dan lima testnya ikut dipertahankan. Audit ulang bug 11 membedakan ilustrasi mint historis, fallback navy, dan MP4 navy; perubahan navy saja tidak membuktikan desain akhir benar. Lihat [audit Audio/Visual](AUDIO_VISUAL_REGRESSION_AUDIT.md).

## Kendala dan kesiapan

Belum dilakukan: Android/emulator nyata, provider picker/storage, process death saat picker terbuka, rendering Word/LibreOffice, suara TTS fisik, playback media, dan integrasi status PRO dengan backend Termux pengguna. Pengujian kontrak memakai fixture/adaptor terisolasi, bukan klaim server produksi aktif.

Risiko yang masih memerlukan keputusan: atribusi pemilik draf lama; kontrak identitas/expiry dan perilaku backend nyata harus diperiksa sebelum rilis. Bug 11 juga masih memerlukan konfirmasi area visual dan perangkat. Tidak ada klaim semua aspek produksi sudah diverifikasi.

Kode lokal siap direview dan dikompilasi menjadi APK PENGUJIAN setelah izin eksplisit. Rilis produksi menunggu batas verifikasi di atas. **Push: BELUM DIIZINKAN. Build APK: BELUM DIIZINKAN.**

Daftar lengkap file kode dan test berubah tersedia pada `source_and_test_files` dalam [results.json](verification/results.json). Checkpoint aktif: [KARIRMATE_STEP3_TO_FINAL_CHECKPOINT.md](KARIRMATE_STEP3_TO_FINAL_CHECKPOINT.md).
