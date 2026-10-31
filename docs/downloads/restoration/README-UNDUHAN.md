# Paket review KarirMate — 31 Oktober 2026

Laporan utama: **Laporan-Audit-Restorasi-KarirMate.pdf** (9 halaman A4, teks dapat diseleksi).

- Android draft PR: https://github.com/ilhamramdhani763-stack/automate-jobs-android/pull/2
- Backend draft PR: https://github.com/ilhamramdhani763-stack/automate-jobs-backend/pull/2
- Branch pada kedua repository: `fix/karirmate-restoration`. Tidak di-merge.

Repository bersifat privat. Untuk mengunduh dari HP, masuk ke GitHub menggunakan akun yang memiliki akses, buka tautan file dengan `?raw=1`, lalu simpan melalui Unduhan browser. PDF dan setiap patch/bundle disediakan sebagai file terpisah. ZIP berisi semua komponen untuk satu kali unduh. Ini adalah file dari repository review, bukan tautan ke direktori workspace sementara.

## Hasil akhir

91 tes Android/JVM/UI PASS; 46 tes backend PASS; 13 tes kontrak Python PASS. Kotlin/lint/kompilasi sumber tes perangkat PASS; lint 71 warning, 0 error. Pengujian HP nyata, OAuth produksi, pengiriman nyata dan cron NOT TESTED. Percobaan capture frame Robolectric FAIL karena redraw timeout, dijelaskan dalam PDF; semua tes perilaku dijalankan kembali dan PASS.

Tidak ada APK, aktivasi email produksi, perubahan token/cron/database produksi atau merge. Modul identity/membership/bank dan source worker Gmail masih diperlukan untuk verifikasi produksi. Kesiapan rilis/build APK ditahan untuk review serta persetujuan pengguna.

## Isi dan integritas

- PDF dan Markdown: audit, akar masalah per file, perbaikan, hasil, keterbatasan, daftar berkas dan referensi resmi.
- `karirmate-android-restoration.patch` / `.bundle`: commit implementasi `f1664e258d0e9394521c4d3414a18ec07232e16c`.
- `karirmate-backend-restoration.patch` / `.bundle`: commit implementasi `42496e044896043f7e03484eb3f9471d1f2b0af0`.
- `change-inventory.json`: commit dasar/implementasi dan daftar berkas.
- `test-results.json`: ringkasan hasil tes dan batas pengujian.
- `source-inventory.json`: metadata sepuluh berkas utama Termux, tanpa token atau data pengguna.
- `SHA256SUMS`: checksum komponen terpisah. ZIP adalah pembungkus dari komponen tersebut.

Kedua bundle bersifat **inkremental**. Bundle memerlukan commit dasar main yang tercantum di bawah. Bundle dan patch tidak menyertakan commit pengemasan paket unduhan ini dan tidak membawa seluruh riwayat repository lama. Gunakan salah satu cara berikut, bukan patch dan bundle sekaligus.

## Meninjau patch di checkout terpisah

Android:

```sh
git fetch origin main
git switch -c review/restoration 2d751b6ec584e3706f758826ce22fe3868f9dc3c
git am /lokasi/karirmate-android-restoration.patch
```

Backend, dari repository backend:

```sh
git fetch origin main
git switch -c review/restoration dbfb30120a302da8a045e2d3e50e9ffa2f6ec193
git am /lokasi/karirmate-backend-restoration.patch
```

## Alternatif bundle

Dari repository yang sesuai, setelah commit dasar tersedia:

```sh
git bundle verify /lokasi/karirmate-android-restoration.bundle
git fetch /lokasi/karirmate-android-restoration.bundle refs/heads/fix/karirmate-restoration:refs/heads/review/restoration-bundle
git switch review/restoration-bundle
```

Untuk backend gunakan file `karirmate-backend-restoration.bundle` di repository backend. Verifikasi tidak mengirim email atau menjalankan layanan. Jangan menjalankan sender dengan `--send`, mengubah cron, atau membangun APK sebagai bagian dari penerapan paket ini.
