# Kelanjutan KarirMate setelah error Cloud — 2026-11-01

Branch: `feat/karirmate-national-interview`, base Android
`f40e217ac8a08bfb68abf8064d9913796a4a3718`. Source dan perubahan lama dipertahankan.

- `SharedCvParser` kini dipakai oleh CV Analyzer, konteks interview CV aktif, dan
  validasi CV Auto Apply. Menjaga fakta bagian bilingual serta membaca proyek
  dan sertifikasi; konteks model menghilangkan field kontak yang tidak diperlukan.
- Layar hasil interview menampilkan alasan fallback evaluasi, serta feedback dan
  kutipan bukti per-jawaban dari LLM untuk PRO. Metode aktual tetap terlihat.
- Tiga tes parser baru serta satu tes kontrak pengiriman konteks CV ditambahkan.
- `:app:testDebugUnitTest`: **103 tes, 0 gagal/error/skipped**, termasuk seluruh
  regresi Robolectric/Compose dan tes baru. Kompilasi Kotlin aplikasi dan tes juga
  berhasil sebagai bagian dari unit test. Kontrak Python: **13 tes lulus**.

Log akhir dan ringkasan JSON ada di direktori ini. Backend pasangannya memiliki
99 tes lulus, importer yang dapat dipratinjau, parser bersama, evaluasi Ollama yang
divalidasi, serta penguatan konfirmasi Gmail dan penggantian CV. Laporan lengkap:
[backend PROGRESS.md](https://github.com/ilhamramdhani763-stack/automate-jobs-backend/blob/feat/karirmate-national-interview/docs/resume/PROGRESS.md).

Hambatan awal proxy Gradle/SDK/cache Robolectric diatasi hanya pada lingkungan
pengujian, tanpa mengganti dependency proyek. Untuk image Cloud yang sama,
`gradle-cloud-tests.init.gradle` meneruskan proxy/trust store ke JVM tes dan
memakai cache writable. Siapkan direktori `/workspace/robolectric-user`, Android
SDK, `ANDROID_USER_HOME`, `ANDROID_HOME`, dan `GRADLE_USER_HOME` pada lokasi
writable. Properti `http.proxyHost`, `https.proxyHost`, port dan trust store harus
sesuai kebijakan Cloud sesi, bukan diisi dengan kredensial. Jalankan hanya:

```bash
./gradlew --init-script docs/resume/gradle-cloud-tests.init.gradle :app:testDebugUnitTest
```

Kredensial Git terminal ditolak setelah pemeriksaan awal. Commit disimpan lewat
konektor GitHub; tree diverifikasi identik dengan source lokal yang diuji dan
branch diperbarui fast-forward dengan expected SHA. Salinan commit lokal awal
tersimpan dalam branch `backup/resume-local-before-api-save`.

Workbook pemilik, layanan/model Ollama, source worker Gmail/OAuth dan runtime
akun/PRO produksi belum tersedia. Uji Gmail/LLM memakai transport simulasi;
respons model nyata, refresh token dan inbox belum diuji. Pengujian perangkat
fisik/voice/avatar belum dilakukan. Tidak merge, deploy, mengirim email nyata,
mengaktifkan runtime produksi, atau build APK/AAB.

Workspace Cloud baru tidak membawa perubahan lokal sesi lama. Perubahan lama
yang belum di-commit tidak dapat dipastikan; perubahan sesi ini disimpan ke GitHub.
