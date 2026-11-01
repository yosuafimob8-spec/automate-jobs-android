# Unduhan KarirMate — Nasional, Auto Apply & AI Interview

Tanggal: 1 November 2026. Branch: feat/karirmate-national-interview.

PDF ini berisi laporan pengembangan lanjutan, jumlah data yang benar-benar terbaca, akar masalah, perubahan, hasil tes, daftar berkas, dependensi dan risiko. Laporan restorasi sebelumnya tetap ada di docs/downloads/restoration dan docs/restoration; PR #2 tidak diubah.

## Simpan di Android

Buka tautan PDF di percakapan dan gunakan tombol unduh/Simpan pada penampil PDF Android. Salinan tersimpan juga ada di GitHub pada branch ini, bukan hanya filesystem Cloud. Karena repository privat, masuk menggunakan akun GitHub yang memiliki akses jika diminta. Pilih “Download raw file” pada halaman berkas bila browser menampilkan preview.

File terpisah: PDF laporan, patch Android, patch backend, Git bundle Android, Git bundle backend. ZIP menggabungkan semuanya beserta laporan teks, CSV kontak bersumber, audit lisensi, operasi dan checksum. Tidak berisi APK, database pengguna, workbook pemilik, token atau avatar/model.

## Patch dan bundle incremental

Android base: c20564aaa9670ea718bddff6c9005db2e1ac3c70
Android implementasi: 87159e5e093807a92544150fa8dc553bea00ed2f
Backend base: 42496e044896043f7e03484eb3f9471d1f2b0af0
Backend implementasi: a380afbe34522eca11804a24f5d400ef0f69416e

Keduanya menyertakan perubahan baru terhadap base fix/karirmate-restoration. Bundle incremental memerlukan commit base tersebut dan tidak membawa seluruh riwayat repository. File hanya sampai commit implementasi, tanpa commit packaging unduhan berikutnya. Patch dan bundle adalah alternatif; jangan terapkan keduanya pada checkout yang sama.

Untuk meninjau bundle dari clone repository yang sesuai:

```bash
git fetch origin fix/karirmate-restoration
git bundle verify /path/FILE-national-interview.bundle
git fetch /path/FILE-national-interview.bundle refs/heads/feat/karirmate-national-interview:refs/heads/review/national-interview
git switch review/national-interview
```

Ganti FILE dengan android atau backend dan /path dengan lokasi unduhan. Perintah tidak merge atau deploy. Untuk patch sebagai alternatif, buat branch review dari commit base lalu `git am /path/FILE-national-interview.patch`.

## Integritas dan status

`sha256sum -c SHA256SUMS.txt` memeriksa file unduhan. Checksum di dalam ZIP mencakup isinya; checksum di luar ZIP juga mencakup ZIP itu sendiri.

PASS: 99 tes Android/Robolectric, 79 tes backend, 13 tes kontrak Python; kompilasi Kotlin dan tes instrumentasi; lint 0 error/71 warning. HP fisik, Gmail produksi, model AI, suara dan avatar nyata NOT TESTED/BLOCKED. Tidak ada APK/build workflow APK, merge, deploy, perubahan cron/token/DB produksi atau email sungguhan.

Snapshot pencarian: 113 nama perusahaan/116 email bersumber; 75 langsung di situs perusahaan, 41 rujukan BKK yang perlu verifikasi. 55 menerima lamaran menurut konteks, 30 sekaligus memiliki lokasi. Kontak baru mencakup 10 dari katalog 38 provinsi; belum 100 penerima resmi siap Auto Apply. Workbook pemilik `~/automate-jobs-v2/perusahaan_jabodetabek_gabungan.xlsx` belum tersedia: angka historis 135/142 belum diaudit ulang.

PR baru: Android https://github.com/ilhamramdhani763-stack/automate-jobs-android/pull/3 ; backend https://github.com/ilhamramdhani763-stack/automate-jobs-backend/pull/3 . Keduanya draft dengan base fix/karirmate-restoration. Produksi tetap ditahan.
