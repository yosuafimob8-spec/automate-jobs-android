# Latihan Psikotes V2 — Backend API

## Status dan cakupan

Backend Latihan Psikotes berjalan sebagai layanan lokal terpisah pada
**`http://127.0.0.1:8789/`** dan hanya bind IPv4 loopback. Layanan ini tidak
menambah rute ke API Cari Lowongan V2 di 8788 dan tidak mengubah API lama 8787.

- Database: `data/psychotest_v2.sqlite3` (mode file `0600`, SQLite lokal).
- Bank soal: **439 soal aktif**, orisinal untuk proyek ini, tanpa soal dummy.
- Mode: **57 mode tersedia**, termasuk **45 mode SKB** untuk **9 formasi**.
- Materi SKB: lampiran BKN/PANRB **2024**; mode gabungan berlabel **Simulasi
  Latihan** karena format resmi per formasi belum terverifikasi.
- Aset Android/APK tidak diubah. Integrasi UI Android belum diuji dengan APK nyata.
- Email, Telegram, Hermes, cron, token, Pro/Super Admin, dan database akun
  produksi tidak disentuh.

## Menjalankan dan menguji

```sh
cd /data/data/com.termux/files/home/automate-jobs-v2
python -B psychotest_api.py
```

Server menulis log ke stdout. Untuk menjalankan di latar belakang dari Termux:

```sh
nohup python -B psychotest_api.py > "$TMPDIR/psychotest_api.log" 2>&1 &
```

Pengujian offline (temporary database, tanpa akun produksi):

```sh
python -B -m unittest -v test_psychotest
python -B -m unittest discover -v
```

Pemeriksaan publik setelah server aktif:

```sh
curl -fsS http://127.0.0.1:8789/health
curl -fsS http://127.0.0.1:8789/psychotests/catalog
curl -fsS 'http://127.0.0.1:8789/psychotests/leaderboard?board=all'
```

## Autentikasi

`GET /health`, `GET /psychotests/catalog`, dan `GET /psychotests/leaderboard`
bersifat publik. Endpoint ujian, riwayat, statistik, dan sesi memerlukan header:

```text
Authorization: Bearer <sesi Android yang sudah ada>
```

Layanan hanya membaca identitas melalui bridge **read-only** `GET
http://127.0.0.1:8787/auth/me`. Token tidak disimpan, tidak dicache, tidak
di-refresh, dan tidak diteruskan ke layanan lain. Implementasi login tidak
diubah. Tanpa header, endpoint terlindungi mengembalikan `401 AUTH_REQUIRED`.

## Endpoint

| Metode | URL | Keterangan |
| --- | --- | --- |
| GET | `/health` | Liveness, bind, bank version, jumlah soal/mode, tanpa upstream |
| GET | `/psychotests/catalog` | 57 mode, 9 formasi SKB, kategori, sumber/tahun, durasi, ketersediaan bank, fitur |
| GET | `/psychotests/leaderboard?board=all\|private\|cpns\|skb&test_id=&limit=` | Peringkat anonim per mode/formasi; `limit` 1–50 |
| POST | `/psychotests/sessions` | Mulai ujian; body `{"test_id":"private_logika"}` |
| GET | `/psychotests/sessions` | Sesi aktif milik akun |
| GET | `/psychotests/sessions/{id}` | Ringkasan sesi, progres, sisa waktu |
| GET | `/psychotests/sessions/{id}/questions` | Soal sesi; kunci/pembahasan belum dikirim |
| PUT | `/psychotests/sessions/{id}/answers` | Simpan batch jawaban; body `{"answers":[...],"revision":0}` |
| POST | `/psychotests/sessions/{id}/submit` | Kirim jawaban; skor dihitung server |
| GET | `/psychotests/sessions/{id}/result` | Hasil, kunci, jawaban pengguna, pembahasan, sumber/tahun, analisis kompetensi SKB |
| POST | `/psychotests/sessions/{id}/retry-wrong` | Buat sesi latihan ulang dari soal SKB yang salah/kosong |
| GET | `/psychotests/history?limit=&offset=` | Riwayat milik akun sendiri |
| GET | `/psychotests/stats` | Statistik, tren, kategori kuat/lemah, akurasi |

`{id}` adalah 32 karakter heksadesimal. Semua respons JSON memiliki
`ok` dan `api_version: 1`. Error berbentuk:

```json
{"ok": false, "api_version": 1, "error": {"code": "SESSION_FINISHED", "message": "..."}}
```

Batch jawaban maksimum 110 item. Setiap item berisi `question_id` dan
`option_id`. Opsi `revision` bersifat opsional; jika dikirim dan tidak cocok,
server mengembalikan `409 REVISION_CONFLICT` untuk mencegah timpa progres.

## Contoh alur JSON untuk Android

Android pada perangkat yang sama memakai `http://127.0.0.1:8789/`. Untuk
pengujian dari komputer melalui ADB, teruskan port tanpa membuka jaringan:

```sh
adb forward tcp:8789 tcp:8789
```

Mulai sesi:

```http
POST /psychotests/sessions
Authorization: Bearer <sesi Android>
Content-Type: application/json

{"test_id": "private_logika"}
```

Respons ringkas:

```json
{
  "ok": true,
  "api_version": 1,
  "session": {
    "id": "0123456789abcdef0123456789abcdef",
    "test_id": "private_logika",
    "status": "active",
    "started_at": 1760000000,
    "deadline_at": 1760000600,
    "remaining_seconds": 600,
    "question_count": 20,
    "answered_count": 0,
    "revision": 0
  },
  "questions_url": "/psychotests/sessions/0123456789abcdef0123456789abcdef/questions"
}
```

Ambil soal. Kunci, skor opsi, dan pembahasan tidak ada pada respons ini:

```http
GET /psychotests/sessions/{id}/questions
Authorization: Bearer <sesi Android>
```

```json
{
  "ok": true,
  "api_version": 1,
  "questions": [
    {
      "position": 1,
      "question_id": "private-logika-01",
      "category": "logika",
      "category_label": "Logika",
      "difficulty": "sedang",
      "prompt": "...",
      "options": [{"id": "B", "text": "..."}, {"id": "A", "text": "..."}],
      "selected_option_id": null
    }
  ]
}
```

Simpan satu atau beberapa jawaban. Kirim `revision` dari respons terakhir agar
perubahan dari perangkat lain tidak tertimpa:

```http
PUT /psychotests/sessions/{id}/answers
Authorization: Bearer <sesi Android>
Content-Type: application/json

{"revision": 0, "answers": [{"question_id": "private-logika-01", "option_id": "A"}]}
```

```json
{"ok": true, "api_version": 1, "saved": 1, "revision": 1,
 "session": {"id": "...", "answered_count": 1, "remaining_seconds": 580}}
```

Kirim ujian, lalu ambil hasil:

```http
POST /psychotests/sessions/{id}/submit
Authorization: Bearer <sesi Android>

GET /psychotests/sessions/{id}/result
Authorization: Bearer <sesi Android>
```

Respons hasil menyertakan `score`, `percent`, `duration_used`, `categories`,
`questions` dengan `correct_option`, `selected_option_id`, `explanation`, dan
`reference` (sumber/tahun/halaman) untuk SKB. Untuk SKB, `analysis` memuat
`weakest_competencies`, daftar soal untuk diulang, dan URL `retry-wrong`.
Untuk `private_kepribadian`, `score`/`percent` bernilai `null` dan hasil berada
pada `personality`; ini bukan diagnosis.

Kesalahan memakai status HTTP yang sesuai dan kode stabil, misalnya
`401 AUTH_REQUIRED`, `409 REVISION_CONFLICT`, `409 ALREADY_SUBMITTED`, atau
`404 SESSION_NOT_FOUND`. Aplikasi sebaiknya menampilkan timer dari
`remaining_seconds`/`deadline_at` server, bukan menyimpan batas waktu sendiri.

## Alur ujian

1. `POST /psychotests/sessions` memilih soal secara acak dari bank aktif dan
   membuat `deadline_at` berdasarkan waktu server.
2. `GET .../questions` mengembalikan soal tanpa kunci, skor opsi, atau pembahasan.
3. `PUT .../answers` menyimpan jawaban secara idempoten per soal. Satu soal hanya
   boleh muncul sekali dalam satu batch.
4. `POST .../submit` menghitung hasil server-side. Jika waktu habis, hasil
   otomatis dibuat dengan sisa jawaban sebagai kosong.
5. `GET .../result` hanya tersedia setelah ujian selesai. Kunci, skor TKP,
   pembahasan, dan interpretasi kepribadian baru terlihat di sini.
6. Timer tidak dapat diperpanjang oleh klien. `remaining_seconds` dihitung dari
   `deadline_at` server.
7. `POST .../retry-wrong` pada sesi SKB yang sudah selesai membuat sesi baru
   berisi soal yang salah atau kosong. Sesi ulang tetap milik akun yang sama.

Sesi aktif dapat dilanjutkan. Sesi selesai tidak menerima jawaban baru dan tidak
bisa dikirim dua kali. ID sesi akun lain menghasilkan `404 SESSION_NOT_FOUND`.

## Durasi dan komposisi mode

| Mode | Soal | Durasi |
| --- | ---: | ---: |
| `private_logika`, `private_numerik`, `private_verbal`, `private_deret`, `private_ketelitian`, `private_spasial`, `private_kepribadian` | 20 | 10 menit |
| `private_combined` | 100 | 50 menit |
| `cpns_twk`, `cpns_tiu`, `cpns_tkp` | 20 | 10 menit |
| `cpns_skd` | 110 (TWK 30, TIU 35, TKP 45) | 100 menit |
| `skb_{formasi}_single` | 20 (7/7/6 per materi) | 10 menit |
| `skb_{formasi}_{materi}` | 7 | 5 menit |
| `skb_{formasi}_combined` | 21 (7 per materi) | 25 menit |

Tes satuan CPNS adalah latihan singkat 20 soal. Bank kategori CPNS tetap memuat
30 TWK, 35 TIU, dan 45 TKP agar simulasi `cpns_skd` memakai komposisi resmi.
Kepribadian tidak memiliki benar/salah dan tidak masuk papan peringkat.

Sembilan formasi SKB bawaan: Administrasi Pemerintahan, Analis SDM Aparatur,
Pranata Komputer, Pengelola Keuangan, Analis Kebijakan, Guru, Tenaga Kesehatan
(Perawat Ahli Pertama), Hukum, dan Arsiparis. Setiap formasi memiliki 3 materi
dan 21 soal unik. Mode `single` memakai 20 soal, mode materi memakai 7 soal,
dan mode gabungan memakai seluruh 21 soal. Mode gabungan **bukan simulasi
resmi** karena jumlah soal, durasi, dan penilaian resmi per formasi tidak
tercantum dalam lampiran materi 2024.

## Penilaian

### Psikotes swasta

- Satu jawaban benar = 1 poin; salah/kosong = 0.
- Skor maksimum 20 untuk tes satuan dan 100 untuk gabungan.
- `percent` = skor / maksimum × 100.
- Hasil menyertakan jumlah benar, salah, kosong, dan pembahasan.

### CPNS TWK/TIU

- Satu jawaban benar = 5 poin; salah/kosong = 0.
- Tes satuan maksimum 100; simulasi SKD: TWK maksimum 150, TIU maksimum 175.

### CPNS TKP

- Setiap opsi memiliki skor 1–5; tidak ada benar/salah.
- Tes satuan maksimum 100; simulasi SKD TKP maksimum 225.
- Rubrik menjelaskan kualitas respons pada hasil.

### SKB per formasi

- Satu jawaban benar = 1 poin; salah/kosong = 0.
- `percent` = skor / maksimum × 100.
- Setiap soal memiliki `reference` berisi sumber, tahun 2024, halaman, dan
  bagian materi. Kunci/pembahasan hanya tersedia setelah submit.
- `analysis.weakest_competencies` menampilkan materi dengan capaian terendah.
- `POST .../retry-wrong` membuat sesi baru dari soal yang salah atau kosong.
- Peringkat dipisahkan per `test_id` (formasi dan mode); skor formasi berbeda
  tidak digabung. Statistik menyediakan `by_formation`.

Katalog SKB juga memuat `verification` dan catatan bahwa sumber 2024 tidak
otomatis menjadi format resmi 2026.

### Simulasi CAT SKD

Acuan yang ditampilkan adalah **Kepmen PANRB No. 321 Tahun 2024**:

- TWK minimum 65 dari 150.
- TIU minimum 80 dari 175.
- TKP minimum 166 dari 225.
- Skor maksimum total 550.

Ambang batas hanya menunjukkan pemenuhan batas nilai. Peringkat formasi dan
kelulusan final tidak disimulasikan. Hasil bukan jaminan kelulusan.

### Kepribadian

20 pernyataan dengan skala 1–5, termasuk item reverse-scored. Hasil berupa lima
dimensi, persentase kecenderungan, level, dua pola dominan, dan disclaimer.
Ini bukan diagnosis psikologis, bukan penilaian benar/salah, dan tidak
menggantikan asesmen profesional.

## Riwayat, statistik, dan peringkat

Riwayat dan statistik hanya membaca sesi milik `subject` Android yang
terautentikasi. Statistik mencakup jumlah tes, rata-rata, nilai terbaik,
perkembangan, akurasi objektif, kategori kuat/lemah, dan tren 30 ujian terakhir.
TKP dan kepribadian tidak dihitung sebagai benar/salah.

Peringkat memakai alias anonim turunan hash; tidak ada email, `account_id`, atau
data pribadi. Papan `private`, `cpns`, dan `skb` terpisah. Setiap formasi dan
mode SKB memiliki papan sendiri sehingga skor formasi berbeda tidak
dibandingkan. Urutan: skor tertinggi, durasi terpendek, pengiriman paling awal,
lalu alias. Kepribadian tidak diperingkatkan. Setiap akun hanya menampilkan
hasil terbaiknya per tes. Sesi **latihan ulang soal salah tidak masuk papan
peringkat** karena soalnya adalah subset yang lebih mudah.

## Katalog SKB dinamis

Formasi tambahan dapat dimuat dari `data/skb_catalog.json` (atau path pada env
`PSYCHOTEST_SKB_CATALOG`). Formatnya object `formations` berisi pack dengan
tepat 3 materi dan tepat 7 soal per materi. Setiap soal memakai opsi pertama
sebagai jawaban benar pada data sumber; server menyusun ulang posisi opsi dan
menyimpan kunci secara aman. Untuk paket 2026, isi `source.year` dengan `2026`;
`verification.material_year` mengikuti tahun sumber dan `format_2026` tetap
`unverified` sampai format resmi per formasi tersedia.

```json
{"formations":[{"id":"contoh_formasi","label":"Contoh Formasi",
  "official_role":"Jabatan Resmi Ahli Pertama",
  "source":{"year":2024,"title":"...","url":"...","checked_on":"2026-10-30"},
  "materials":[{"id":"materi_satu","label":"Materi Satu","page":1,
    "topics":["topik"],"questions":[{"prompt":"...","options":["benar","salah 1","salah 2","salah 3"],
    "explanation":"...","difficulty":"sedang"}]}]}]}
```

## Keamanan dan batas

- Bind loopback saja; port 8787/8788 ditolak oleh constructor layanan.
- Body maksimum 256 KiB; jawaban maksimum 110 per batch; query ketat.
- Rate limit global 240 permintaan/menit dan 32 koneksi bersamaan.
- Tidak ada penyimpanan token, email keluar, auto-apply, atau akses upstream acak.
- `GET /health` tidak memeriksa jaringan/upstream.
- Database dan test memakai path lokal V2 atau direktori temporary yang disetujui.

## Verifikasi terakhir

- **202 tes keseluruhan lulus**, termasuk **35 tes psikotes**.
- Bank tervalidasi: 439 ID unik, 37 kategori, 57 mode tersedia (9 formasi SKB).
- Database aktif: integrity `ok`, 439 soal aktif, 0 sesi produksi.
- Uji HTTP nyata pada `127.0.0.1:8789`: health, catalog, dan leaderboard publik
  `200`; endpoint riwayat tanpa auth `401`; endpoint tidak dikenal `404`.
- Uji alur ujian lengkap, timer, auto-submit, isolasi akun, proteksi kunci,
  TKP, kepribadian, CPNS SKD, SKB 9 formasi, analisis kelemahan, latihan ulang
  soal salah, riwayat, statistik per formasi, dan leaderboard dilakukan dengan
  **database temporary + identitas sintetis**, bukan akun produksi.
- **Token Android nyata belum tersedia**, sehingga integrasi akun/UI APK belum
  dapat diklaim selesai.
- Bukti ringkas: `job_sources/psychotest_verification.json`.
