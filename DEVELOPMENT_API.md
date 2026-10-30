# Konfigurasi API Pengembangan

Android tidak boleh menggunakan `127.0.0.1` untuk API yang berjalan di komputer pengembang karena alamat tersebut menunjuk ke perangkat Android sendiri.

Untuk Android Emulator, default debug API adalah `http://10.0.2.2:8787/`, yang meneruskan koneksi ke host pengembang. Untuk perangkat fisik, berikan URL HTTPS yang dapat dijangkau perangkat melalui properti Gradle lokal yang tidak dikomit:

```properties
debugApiBaseUrl=https://server-pengembangan-anda.example/
```

Jangan gunakan HTTP untuk server yang dapat dijangkau di luar mesin pengembangan. Konfigurasi rilis tetap memakai `releaseApiBaseUrl` dan default HTTPS yang telah ada.

Layanan lowongan V2 pada 8788 dan psikotes pada 8789 belum memiliki kontrak endpoint, model, atau URL HTTPS yang dicatat dalam proyek Android. URL tersebut sengaja tidak ditebak dan belum dihubungkan sampai tersedia kontrak berversi (misalnya OpenAPI) serta alamat server yang aman.
