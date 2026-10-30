# AutoMate Jobs V2 — pencarian Jabodetabek

## HTTP API Android V2 — aktif di loopback 8788

Server baru `http_api.py` membungkus mesin V2 yang ada, menggunakan Python stdlib:

```sh
AUTOMATE_JOBS_V2_UPSTREAM_INTERVAL=0.6 python -B http_api.py
```

Tanpa variabel pacing, default aman 1,0 detik per host. Base URL
**`http://127.0.0.1:8788/`**. Endpoint GET aktif: `/health`, `/jobs`,
`/job-categories`, `/locations`, `/job-sources`. Port 8787 tetap dicadangkan bagi
API lama; server V2 tidak membuka listener internet.

`/jobs` mendukung multi-jabatan/lokasi/kategori sumber, kategori jabatan,
tanggal (default 30 hari), ID sumber, pagination snapshot, dan `format=legacy`
untuk respons array Android lama. Cache HTTP, single-flight, batas request,
timeout, penanganan error JSON, dan status sumber parsial tersedia.

Katalog lokal V2 berisi **135 perusahaan, 142 email unik, dan 142 pasangan
perusahaan-email** dari workbook asli (284 observasi lintas dua sheet). Semua
kontak tetap **belum diverifikasi independen**; email tidak dikirim. `/jobs`
menggabungkan lowongan LIVE dengan **peluang lamaran spontan** berlabel referensi
jabatan, tanpa tanggal/gaji karangan. Header `Authorization` opsional diverifikasi
read-only ke `/auth/me` API lama dan menerapkan batas **2 posisi per perusahaan
per akun** lintas halaman/snapshot/query. Tanpa header, API tetap publik.

**167 tes lulus**, termasuk tes katalog/identitas, fallback katalog tanpa store, batas posisi/lamaran, dan pacing upstream.

Impor ulang bersifat idempoten dan hanya menulis storage lokal V2:

```sh
python -B import_companies.py --report job_sources/company_import_verification.json
```
Verifikasi final 29 Oktober 2026 22:03–22:08 UTC pada kode baru: **28/28
pemeriksaan HTTP lulus**, **74 lowongan LIVE Jabodetabek**, **675 referensi
spontan (749 total)**, **17 sumber API berhasil**, dan 94 request upstream.
Pencarian dingin turun dari 92,7 menjadi **60,2 detik** (-35,1%) dengan hasil
yang sama. Empat portal tetap `authorization_required` dan menghasilkan 0 job.
Uji dengan token Android nyata belum dilakukan; batas dua posisi/lamaran
diverifikasi memakai server ephemeral + identitas sintetis.

Dokumentasi Android lengkap (parameter, alias, JSON, Retrofit/Kotlin, kompatibilitas,
timeout, dan pagination): **`ANDROID_API_V2.md`**.
Bukti HTTP final: `job_sources/http_api_verification_v2_final.json`; bukti
historis tetap di `job_sources/http_api_verification.json`.

## Latihan Psikotes V2 — backend lokal 8789

Backend Latihan Psikotes terpisah tersedia di **`http://127.0.0.1:8789/`** dan
hanya bind loopback. Bank berisi **439 soal orisinal**, **57 mode**, termasuk **9 formasi SKB**
dengan 45 mode, database lokal `data/psychotest_v2.sqlite3`, timer dan penilaian
server-side, riwayat/statistik per akun, serta leaderboard anonim terpisah untuk
swasta, CPNS, dan SKB per formasi/mode.
Kunci jawaban dan pembahasan baru dikirim setelah ujian selesai. Layanan ini
tidak menambah rute ke 8788, tidak mengubah 8787, dan tidak mengubah APK.

```sh
python -B psychotest_api.py
python -B -m unittest -v test_psychotest
```

Dokumentasi endpoint, autentikasi, skor, batas privasi, dan status integrasi:
**`PSYCHOTEST_API.md`**. Uji terakhir: **202 tes lulus**, termasuk 35 tes
psikotes; token Android nyata belum tersedia sehingga integrasi UI APK belum
diklaim selesai. Materi SKB mengacu lampiran BKN/PANRB 2024 dan mode gabungan
berlabel **Simulasi Latihan** karena format resmi per formasi belum terverifikasi.
Bukti: `job_sources/psychotest_verification.json`.

## Empat portal pekerjaan — status terbaru

Adapter terpisah untuk **Glints, Indeed Indonesia, Jobstreet Indonesia, dan
LinkedIn Jobs** sudah ditambahkan serta terhubung ke mesin V2 melalui feed JSON
berizin. **Belum ada portal yang lulus pengambilan data LIVE**: izin/koneksi
dataset yang mengizinkan pembacaan dan penayangan belum tersedia. Hasil nyata
yang berhasil dikumpulkan dari masing-masing portal pada pengujian ini: **0**.
Ini bukan klaim bahwa portal tidak memiliki lowongan.

Registry kini **44 sumber**, dengan **17 sumber LIVE sebelumnya tetap**; empat
portal baru masih `authorization_required`/fallback. Delapan permintaan metadata
terbatas (robots dan kebijakan/bantuan, dua per portal) telah dilakukan. Tidak
ada kueri data yang belum diizinkan, auto-apply, atau pemakaian ulang token lama.

Konektor mendukung normalisasi jabatan, perusahaan, lokasi, tanggal, gaji jika
tersedia, URL asli, deduplikasi, filter V2 dan 30 hari, cache, rate limit, timeout,
dan paginasi terbatas. Tujuh metode lamaran tetap dipertahankan. Adapter feed
berizin tidak diklaim sebagai native client seluruh API mitra portal.

Detail evaluasi JobSpy/ID-Jobs, hambatan akses, jalur resmi, skema feed dan cara
menghubungkan koneksi sah: `job_sources/PORTAL_INTEGRATIONS.md`.
Bukti uji terbatas: `job_sources/portal_access_verification.json`.
Seluruh **98 tes lulus**, termasuk 74 pengujian sebelumnya.

```sh
python -B search_live.py --source-ids glints indeed jobstreet linkedin --max-age-days 30
python -B -m unittest -v test_job_search test_live_sources test_application_methods test_source_expansion test_search_quality test_portal_adapters
```

## Audit relevansi dan optimasi filter — 29 Oktober 2026

Snapshot 73 hasil diperiksa terhadap judul, perusahaan, lokasi, tanggal,
identitas sumber, URL, dan bukti metode lamaran. **73 valid, 0 ditolak**;
seluruh 73 juga dicocokkan kembali dengan detail API publik sumbernya. Audit
memakai waktu/filter query snapshot asli agar batas tanggal tidak bergeser.
Verifikasi ini memeriksa metadata/API, bukan mengirim lamaran atau memeriksa
isi formulir dengan browser.

Pemindaian daftar dari 17 sumber LIVE membaca **2.223 record**, dengan 73
kandidat cocok dan **2.150 ditolak**. Alasan utama: 1.517 lokasi di luar/tidak
cukup dikenali sebagai Jabodetabek, 502 remote-only, 130 publikasi sebelum batas
tanggal, dan 1 talent pool. Alasan utama dihitung satu per record, sedangkan
detail menyimpan semua kegagalan filter yang ditemukan. Kandidat list bukan
tambahan lowongan yang belum diverifikasi detailnya.

Distribusi valid tetap Jakarta 63, Bogor 2, Depok 0, Tangerang 1, Bekasi 7.
Penyebab jumlah kecil terbukti dari respons sumber:

- Depok memiliki satu posting: Cermati **Offline Promotor**, ID
  `744000029852295`, terbit **3 Desember 2024**. Posting ini dikenali sebagai
  Depok tetapi tidak lolos filter 30 hari.
- Tangerang memiliki tujuh posting sebelum filter tanggal: enam sebelum
  **29 September 2026**, satu lolos. Yang lolos adalah Sampoerna Academy
  **ICT & Computer Science Teacher**, ID `744000157728607`, South Tangerang,
  terbit **26 Oktober 2026**. Tanggal batas lengkap tercatat dalam snapshot.

### Perbaikan lokasi

- Tangerang Selatan/South Tangerang/Tangsel, BSD/Bumi Serpong Damai, dan Gading
  Serpong dikelompokkan ke Tangerang; Cikarang, Cibitung, Tambun ke Bekasi.
- Kota/subarea utama mendahului label induk yang salah: `BSD, Jakarta` tidak
  dianggap Jakarta. Metadata memberi peringatan `conflicting_parent_region`.
- Hierarki eksplisit seperti `Cilandak, South Jakarta` tetap dikenali; provinsi
  umum seperti `West Java` saja tidak cukup untuk menyimpulkan kota.
- Homonim `Depok, Sleman`, `Sawangan, Magelang`, dan `Cibitung, Sukabumi` atau
  Pandeglang ditolak. Karawang/Purwakarta/Serang/Cilegon tidak dimasukkan.
- Filter subarea tidak menerima wilayah saudara: BSD tidak otomatis menerima
  Gading Serpong; Tangerang Selatan tidak otomatis menerima Kota Tangerang.
- `Greater Jakarta`/`Jabodetabek` hanya memiliki presisi metropolitan; dapat
  masuk Semua Lokasi tetapi tidak otomatis diberi label kota Jakarta.
- Alternatif lokasi dipisahkan dengan `;`, `|`, atau `/`. Keterangan luar
  negeri dalam alternatif lain tidak mengubah kota Indonesia menjadi asing.

Hasil baru menambahkan `location_regions` dan `location_precision`. Teks lokasi
asli tetap disimpan. Normalisasi teks, keputusan jabatan, dan parsing lokasi
menggunakan cache LRU terbatas dalam memori; hasil cache tidak dapat dimutasi
melalui objek metadata yang dikembalikan.

### Kategori jabatan tambahan

`get_search_catalog()['role_categories']` menyediakan **OB, Helper, Driver,
Kurir, Chef, Barista, Resepsionis** sebagai kelompok filter tambahan di mode LIVE.
Katalog JSON asli 14 kategori/69 referensi dipertahankan. Contoh:

```sh
python -B search_live.py --categories OB Helper Driver Kurir Chef Barista Resepsionis --locations Jakarta Tangerang Bekasi
```

Kelompok Chef mencakup Head/Sous/Commis/Pastry Chef, Barista mencakup Senior
Barista, dan Resepsionis mencakup variasi front office. Pemilihan jabatan tunggal
tetap konservatif. Istilah teknis (`device driver`, `Chef DevOps`), tenaga medis
`OB/GYN`, serta jabatan penjualan jasa kurir bukan pekerjaan Driver/Chef/OB/Kurir.
Ketujuh kelompok tersebut **tidak memiliki hasil dalam snapshot 73 ini**;
semua 73 sah untuk query Semua Jabatan, bukan berarti cocok dengan tujuh kelompok
tersebut. Tes positif/negatif memakai kasus sintetik terpisah, bukan lowongan baru.

Bukti audit lengkap: `job_sources/relevance_audit.json`. Pengujian lengkap
sekarang **74 tes**, termasuk 50 regresi sebelumnya:

```sh
python -B -m unittest -v test_job_search test_live_sources test_application_methods test_source_expansion test_search_quality

# Audit snapshot offline ke file JSON baru:
python -B audit_search_results.py --output job_sources/audit_ulang.json
# Tambahkan --verify-live --scan-coverage hanya untuk pemeriksaan API eksplisit.
```

## Perluasan sumber terbaru — 29 Oktober 2026

Registry sekarang memuat **40 sumber**, termasuk **lebih dari 30 perusahaan/grup
nyata** tanpa menghitung portal pemerintah sebagai perusahaan. Ada **17 akun
LIVE terverifikasi** (11 SmartRecruiters, 6 Greenhouse) dan **23 tautan resmi**.
Nama perusahaan/HTTP 200 kosong saja tidak cukup untuk mengaktifkan sumber.
Setiap akun LIVE menyimpan bukti `activation` berupa endpoint, HTTP 200, waktu
pengujian, dan dasar pemetaan identitas pada situs resmi. Board ATS duplikat
ditolak agar satu akun tidak dihitung sebagai beberapa perusahaan.

Verifikasi 11:31–11:32 UTC untuk semua jabatan, seluruh Jabodetabek, publikasi
30 hari terakhir: **73 lowongan nyata**, dari **7 sumber yang memiliki hasil
cocok**. Seluruh 17 endpoint terkonfigurasi berhasil dipindai; 10 lainnya tidak
memiliki hasil yang memenuhi filter. Daerah: Jakarta 63, Bogor 2, Depok 0,
Tangerang 1, Bekasi 7. Angka ini snapshot filter, bukan total pasar kerja.
Lokasi multi-wilayah dapat dihitung pada beberapa wilayah; hasil global tetap
dideduplikasi berdasarkan ID sumber/lowongan.

Sumber baru yang diaktifkan: Bosch, NielsenIQ, Avery Dennison, Louis Dreyfus
Company, Syngenta, Ramboll, Eurofins, Informa, Agoda, Datadog, GitLab, Twilio,
dan Stripe. Integrasi Cermati, Xendit, Sampoerna Schools System, dan Putera
Sampoerna Foundation dipertahankan. Seluruh filter dan tujuh metode tindakan
lamaran tetap berlaku. URL khusus perusahaan yang belum terbukti formulirnya
tetap memakai “Lihat Sumber Resmi”, termasuk halaman kustom Datadog.

Status lengkap serta alasan sumber belum LIVE: `job_sources/SOURCE_STATUS.md`.
Bukti hasil: `job_sources/expansion_verification.json` (73 hasil lengkap),
dan proyeksi respons API untuk regresi: `job_sources/expansion_samples.json`.
Tidak ada HEAD Apply dalam verifikasi perluasan ini; keberhasilan API bukan
klaim formulir lamaran dapat diakses. Snapshot sebelumnya tetap disimpan.

```sh
# Pencarian terfokus pada akun tertentu (opsional):
python -B search_live.py --source-ids bosch avery_dennison ldc --max-age-days 30

# Verifikasi luas eksplisit, simpan ke file BARU; tidak menimpa bukti lama:
python -B verify_live_registry.py --output job_sources/check_baru.json --samples job_sources/sampel_baru.json --max-age-days 30 --max-requests 300 --max-seconds 600 --requests-per-source 100 --max-details 100

# Regresi lengkap, tanpa jaringan:
python -B -m unittest -v test_job_search test_live_sources test_application_methods test_source_expansion
```

`source_ids` dapat digunakan bersamaan dengan filter kategori sumber, jabatan,
lokasi, dan tanggal. ID tidak dikenal ditolak sebelum akses jaringan. Budget
per-sumber dan reservasi permintaan awal mencegah perusahaan besar menghabiskan
seluruh kuota sumber berikutnya. Budget total/deadline tetap berlaku; hasil
yang dibatasi dilabeli `partial`, bukan dianggap pencarian lengkap.

## Pemakaian backend

### Pencarian LIVE dan fallback resmi

```python
from search_engine import search_jobs_with_status, search_jobs

report = search_jobs_with_status(
    positions='Semua Jabatan',
    locations='Semua Lokasi Jabodetabek',
    sources='Semua Sumber',
    max_age_days=30,
    limit=12,
    check_apply=True,
)
jobs = report['jobs']
official_search_links = report['fallback_links']
source_health = report['source_status']
counts = report['summary']

# Kontrak list untuk pemanggil lama; status dan fallback lewat fungsi report.
jobs_only = search_jobs(['Data Analyst', 'Quality Assurance'],
                       ['Jakarta', 'Bekasi'], live=True, max_age_days=30)
```

`search_jobs()` tanpa `live=True` tetap mencari feed lokal untuk kompatibilitas.
Import modul/katalog tidak melakukan permintaan jaringan. Mode LIVE tidak membaca
`config.py`, token, atau database. Fallback adalah entri `official_search_link`
terpisah, tidak pernah dimasukkan sebagai lowongan dan tidak ikut hitungan hasil.
`filters_applied: false` berarti pengguna perlu mengisi filter di portal tujuan;
parameter pencarian portal tidak dikarang.

### Feed lokal yang sudah ada

```python
from search_engine import search_jobs, get_search_catalog

catalog = get_search_catalog()  # dictionary siap diserialisasi menjadi JSON
jobs = search_jobs(
    positions=['Admin Gudang', 'Data Analyst'],
    locations=['Bekasi', 'Jakarta'],
    sources=['job_portal'],
    limit=12,
)
jobs_all = search_jobs('Semua Jabatan', 'Semua Lokasi Jabodetabek', 'Semua Sumber')
jobs_it = search_jobs(categories=['IT & Digital'], locations=['Jakarta', 'Depok'])
```

- Katalog asli: 14 kategori, 69 referensi jabatan (67 nama unik), lima wilayah.
- Parameter tunggal tetap menerima string; beberapa pilihan memakai list string.
- `None` atau list kosong berarti semua. Label Semua mengalahkan pilihan lain
  dalam filter yang sama. Semua lokasi tetap terbatas pada Jabodetabek.
- Antarjabatan, antarlokasi, dan antarsumber menggunakan OR; antara kelompok
  filter menggunakan AND. `categories` menambahkan referensi jabatan ke pilihan.
- Sinonim Indonesia/Inggris tersedia untuk seluruh referensi katalog. Pencocokan
  berdasarkan judul; jabatan bebas dicocokkan sebagai frasa, bukan pencarian fuzzy.
- Feed lokal tetap menyediakan fallback jabatan lama melalui `match_label`;
  mode LIVE menggunakan pencocokan judul/sinonim tanpa alternatif terkait.
- Lokasi di luar katalog dan kategori/sumber tidak dikenal menghasilkan `ValueError`.
- `fetch_feed(position, location, limit=12)` dan `fetch_remote(position, location=None)`
  tetap tersedia. Pencarian Jabodetabek tidak mencampurkan lowongan remote
  Himalayas ke hasil lokasi kota. Pilihan Semua Jabatan tidak terbatas pada 69
  referensi katalog, sehingga juga mencakup jabatan guru, manajer, dan lainnya.

## Status sumber (bukan enam konektor aktif)

| ID kategori | Status saat ini |
| --- | --- |
| `company` | 15 akun LIVE; lihat status lengkap di `job_sources/SOURCE_STATUS.md` |
| `bumn` | Tautan resmi Pertamina, Telkom, PLN, dan BRI; belum ada pengambilan otomatis |
| `government` | Tautan Karirhub Kemnaker dan formasi SSCASN BKN; belum ada pengambilan otomatis |
| `healthcare` | Tautan resmi Siloam, Hermina, Mayapada, RSUI; belum ada pengambilan otomatis |
| `education` | LIVE: Sampoerna Schools System dan Putera Sampoerna Foundation; tautan BINUS/UI |
| `job_portal` | Tautan Kalibrr; feed lokal ID-Jobs Dealls/Kalibrr tetap tersedia terpisah |

Registry yang dapat diperluas ada di `job_sources/registry.json`: 44 sumber,
17 akun sumber API terdahulu, dua jenis adapter aktif, dan 27 sumber yang belum
diaktifkan (termasuk empat konektor portal yang memerlukan feed berizin).
`get_search_catalog()` menyediakan konfigurasi registry dan kemampuan konektor;
`configured_not_checked` bukan klaim berhasil diakses saat itu. Status aktual
`live`, `partial`, atau `unavailable` tersedia pada setiap respons LIVE.
Memilih kategori link-only menghasilkan daftar lowongan kosong plus tautan resmi.
Karirhub adalah portal Kemnaker; tidak semua iklannya merupakan rekrutmen ASN.
Ashby tetap didukung sebagai URL ATS dari feed lama, tetapi tidak otomatis
diklasifikasikan sebagai website perusahaan, BUMN, rumah sakit, atau pendidikan.
Hasil Ashby yang belum diklasifikasikan hanya masuk pilihan Semua Sumber dengan
`source_category: null`.

Adapter hanya menggunakan GET API job-board publik yang terdokumentasi:
https://developers.smartrecruiters.com/docs/endpoints dan
https://developers.greenhouse.io/job-board.html (GET tanpa autentikasi).
Registry menyimpan halaman karier dan bukti pemetaan akun ATS. Tidak ada akses
login, bypass CAPTCHA, submit lamaran, atau penggunaan endpoint internal portal.
Nama rumah sakit pada board publik saja tidak cukup untuk mengaktifkan konektor.
Feed lokal yang dipasok tetap memerlukan hak penggunaan/redistribusi upstream.

Untuk menambah sumber: tambahkan ID unik, kategori, adapter, board ID publik,
URL resmi, bukti identitas ATS, dokumentasi akses, dan allowlist host Apply.
Sertakan `activation` dengan endpoint yang benar-benar diuji, `http_status: 200`,
`checked_at` ISO 8601, dan `identity_basis` (`official_page_ats_reference`,
`matching_official_job_id`, atau `official_domain_redirect`). Pengujian endpoint
dan peninjauan identitas harus dilakukan sebelum menyimpan status LIVE.
Gunakan `link_only` beserta alasan bila akses otomatis belum terverifikasi.
Host API dibatasi pada dua penyedia adapter; adapter baru memerlukan kode dan
pengujian tersendiri, bukan sekadar mengganti URL registry.

### Hasil verifikasi LIVE awal (historis, sebelum perluasan)

Pengujian 29 Oktober 2026, mulai 10:50 UTC, seluruh jabatan/Jabodetabek dan
publikasi 30 hari terakhir:

- Cermati: 28 lowongan cocok.
- Xendit: 2 lowongan cocok (termasuk lokasi multi-negara yang mencantumkan Jakarta).
- Sampoerna Schools System: 5 lowongan cocok.
- Putera Sampoerna Foundation: endpoint berhasil, 0 lowongan cocok.
- Total: **4 sumber API berhasil, 35 lowongan nyata setelah deduplikasi**,
  12 dikembalikan sesuai limit; enam sumber lainnya tetap link-only.
- API sumber selesai dipindai untuk filter ini. Ini bukan total pasar lowongan.
- HEAD Apply ditolak dan memicu cooldown: **0 tautan terbukti HTTP reachable**,
  12 hasil berlabel `http_unverified`. Iklan tetap aktif menurut API penyedia,
  tetapi akses formulir Apply belum terbukti. Tidak ada upaya melewati penolakan.

Snapshot ringkasan: `job_sources/live_verification.json`. Proyeksi field respons
nyata beserta URL bukti untuk pengujian: `job_sources/verified_samples.json`.
Keduanya merupakan bukti historis, tidak dibaca sebagai feed pencarian.

## Integritas hasil dan kompatibilitas Android

Field hasil lama dipertahankan: `title`, `company`, `location`, `position`, `link`,
`source`, `source_link`, `updated`, `remote`, `expiry_ts`, dan `match_label`.
Field tambahan: `matched_positions`, `source_category`, `published_at`,
`first_seen`, `date_kind`, `imported_at`.

Mode LIVE menambahkan `source_id`, `provider_job_id`, `updated_at`, `fetched_at`,
`cache_hit`, `live`, `apply_check`, `apply_http_status`, `apply_checked_at`.
`published_at` menggunakan `releasedDate` SmartRecruiters (tanggal rilis/repost)
atau `first_published` Greenhouse. `updated_at` tidak menggantikan tanggal
publikasi saat memfilter. `since`/`until` menerima ISO 8601; tanggal saja memakai
UTC dan `until` mencakup seluruh hari. `max_age_days` opsional; jika tidak diisi,
tidak ada pembatasan usia publikasi. Tanggal tidak diketahui dikeluarkan ketika
filter tanggal digunakan. Lowongan kedaluwarsa, remote-only, talent pool, serta
iklan SmartRecruiters tidak aktif/nonpublik dikeluarkan.

### Metode pendaftaran dan tombol tindakan

Lowongan LIVE **tidak wajib mempunyai URL Apply**. Hasil baru memuat
`application_method` dan satu tombol utama dalam `application_actions`:

| Metode | Tombol |
| --- | --- |
| `portal_apply` | Lamar di Portal |
| `company_form` | Buka Formulir Perusahaan |
| `email_hr` | Kirim Email ke Rekrutmen Resmi |
| `recruitment_portal` | Buka Portal Rekrutmen |
| `government_registration` | Buka Portal Pendaftaran Pemerintah |
| `announcement_pdf` | Lihat Pengumuman Resmi (PDF) |
| `information_only` | Lihat Sumber Resmi |

Klien merender `application_actions` (field `method`, `label`, `url`), bukan
membuat tombol “Lamar Sekarang” dari keberadaan `link`. `application_url` boleh
null; email ada pada `application_email`, PDF pada `announcement_url`.
`application_evidence_url`, `application_verification`, dan
`application_verified_at` membedakan bukti API, bukti resmi yang ditinjau, dan
tautan informasi saja. `job_url` adalah halaman detail yang tersedia dari sumber.
Field lama `link` tetap HTTPS untuk kompatibilitas, termasuk pada metode email;
tombol email menggunakan `mailto:` di `application_actions`, bukan `link`.

SmartRecruiters `applyUrl` dan halaman/form Greenhouse `absolute_url` yang cocok
dengan identitas board/lowongan menghasilkan `portal_apply`. Halaman pengumuman
biasa, kategori BUMN/pemerintah, atau alamat email di deskripsi tidak otomatis
dianggap metode pendaftaran. URL Apply tidak ada/tidak valid menghasilkan
`information_only`, tanpa membuang lowongannya. Dua iklan dengan ID berbeda
tetap terpisah walaupun tombol informasinya menuju halaman resmi yang sama.

Email HR, formulir khusus, portal pendaftaran, dan PDF memerlukan catatan bukti
yang ditinjau pada registry sumber, di `verified_applications`:

```json
{
  "job_id": "ID_LOWONGAN_YANG_TEPAT",
  "method": "announcement_pdf",
  "target": "https://careers.example.com/pengumuman.pdf",
  "evidence_url": "https://careers.example.com/lowongan/ID_LOWONGAN_YANG_TEPAT",
  "verified_at": "2026-10-29"
}
```

Contoh di atas hanya skema dengan domain contoh, bukan pengumuman nyata.
Peninjau registry harus memastikan dokumen/alamat/tata cara benar-benar disebut
pada sumber resmi untuk **ID lowongan tersebut** sebelum menambahkan catatan.
Flag `verified` dari data lowongan tidak dipercaya. Domain bukti harus milik
sumber resmi; domain tujuan mengikuti allowlist `form_hosts`, `registration_hosts`,
`document_hosts`, atau `apply_hosts` sesuai metode. Email harus cocok dengan
domain resmi atau `recruitment_email_domains` yang telah diverifikasi; alamat
tidak ditebak dan tidak dipanen dari teks. PDF harus merupakan URL dokumen resmi
berakhiran `.pdf`, atau jalur unduhan yang telah diverifikasi memiliki
`content_type: application/pdf`. Saat ini registry produksi tidak menambahkan
alamat HR atau PDF yang belum terverifikasi.

`fallback_application` adalah catatan serupa untuk **tautan manual sumber**,
bukan bukti metode setiap lowongan. Pertamina diarahkan ke portal rekrutmennya;
SSCASN ke portal pembuatan akun yang ditautkan BKN. Karirhub tetap informasi
portal lowongan, bukan otomatis pendaftaran ASN. Fallback tidak menjadi lowongan
dan tidak menambah jumlah hasil.

Pemeriksaan Apply default menggunakan bukti API (`provider_active`) atau catatan
resmi (`method_verified`). Email/PDF/informasi saja memakai `not_applicable` dan
tidak menjalankan HEAD Apply. `check_apply=True` hanya memeriksa metode
pendaftaran web: `http_reachable` atau `http_unverified`. HTTP 404/410 menonaktifkan
tindakan Apply dan mengubah tombol menjadi “Lihat Sumber Resmi” dengan
`apply_check: application_link_closed`; lowongannya tetap ditampilkan.
403/429, redirect, dan timeout bukan bukti lowongan ditutup. HEAD tidak menguji
pengiriman lamaran atau isi formulir. Backend feed/remote lama tidak diubah oleh
lapisan tindakan ini.

Database lama menyimpan `first_seen` dalam kolom `scraped_at`. Nilai ini ditampilkan
sebagai tanggal pertama terlihat, **bukan tanggal publikasi**. `published_at`
tetap null karena feed lama tidak menyimpan tanggal publikasi. Tanggal tidak valid
ditampilkan sebagai `Tidak diketahui`; belum ada verifikasi kedaluwarsa feed lokal.

URL wajib HTTPS, tanpa kredensial/port nonstandar, dan sesuai allowlist sumber.
Duplikat dihapus berdasarkan URL dengan mengabaikan fragmen serta parameter
pelacakan; parameter identitas lowongan dipertahankan. Judul/perusahaan sama
dengan URL berbeda tidak otomatis digabung karena bisa merupakan lowongan berbeda.
Mode LIVE memakai pasangan ID sumber dan ID lowongan agar tautan tindakan
bersama tidak menggabungkan iklan yang berbeda.

### Batas akses dan cache

- Timeout 10 detik per permintaan, jeda minimum satu detik per host.
- Maksimum 100 permintaan jaringan dan deadline 180 detik per pencarian.
- Maksimum 40 permintaan per sumber secara default (`requests_per_source`),
  dengan reservasi permintaan awal bagi sumber LIVE berikutnya. CLI menyediakan
  `--max-requests`, `--max-seconds`, `--requests-per-source`, dan `--source-ids`.
- Maksimum 10 halaman × 100 iklan per sumber dan 40 detail SmartRecruiters per
  sumber secara default (`max_details` dapat diubah). Filter lokasi/jabatan/tanggal
  diterapkan sebelum mengambil detail untuk menghemat permintaan.
- Cache TTL/LRU dalam memori proses: 15 menit, maksimum 512 entri; tidak ada cache
  disk atau perubahan database. `fetched_at` tetap waktu pengambilan asli ketika
  memakai cache. Gunakan proses/client yang sama untuk memanfaatkan cache.
- Kegagalan menerapkan cooldown host; HTTP 429 menghormati `Retry-After` numerik
  dengan batas 60–3600 detik. Tidak ada retry loop atau redirect otomatis.
- Batas detail/halaman/waktu atau kegagalan halaman lanjutan menghasilkan status
  `partial`, mempertahankan hasil yang sudah didapat, dan menambahkan fallback.
  Ringkasan `complete` hanya mencakup sumber API terkonfigurasi yang dipilih,
  bukan keenam kategori/pasar lowongan secara keseluruhan.
- `real_jobs_found` adalah jumlah iklan cocok sebelum limit, termasuk yang hanya
  memiliki informasi resmi. Kegagalan URL Apply tidak mengurangi jumlah iklan;
  `apply_links_closed` menghitung tautan Apply 404/410 yang dinonaktifkan.
  `returned_jobs` adalah panjang list hasil; HEAD hanya dilakukan pada metode
  pendaftaran web dari hasil yang dikembalikan.

Folder ini tidak memuat server/rute API atau kode Android. Kompatibilitas fungsi
Python dan field JSON lama diuji; pemetaan endpoint dan pengujian end-to-end
Android masih memerlukan kode API/aplikasinya. Respons pencarian tetap list,
sedangkan katalog/status disediakan lewat fungsi terpisah.

## Verifikasi dan konfigurasi

```sh
python -B -m unittest -v test_job_search
python -B -m unittest -v test_job_search test_live_sources
python -B -m unittest -v test_job_search test_live_sources test_application_methods
python -B -m unittest -v test_job_search test_live_sources test_application_methods test_source_expansion

# Pengambilan LIVE eksplisit, bukan bagian pengujian offline:
python -B search_live.py --limit 12 --max-age-days 30 --check-apply
python -B search_live.py --positions "Quality Assurance" "Data Analyst" --locations Jakarta Bekasi --sources education company --max-age-days 30
```

Pengujian berjalan offline dengan database sementara di direktori temporary
OpenCode. Pencarian membuka database dengan SQLite `mode=ro`; database/tabel
yang tidak tersedia menghasilkan list kosong tanpa dibuat otomatis. Fungsi
impor eksplisit lama tetap menulis hanya jika dipanggil pengguna.
Pengujian adapter menggunakan proyeksi respons nyata yang dapat diverifikasi;
perubahan data pada negative tests sengaja menguji iklan tidak aktif, kedaluwarsa,
domain palsu, kegagalan jaringan, paginasi, cache, cooldown, dan batas akses.

`config.py` membaca rahasia dari file lokal, tanpa literal token/kunci. File
rahasia, database, dan pengaturan lokal sudah dimasukkan ke `.gitignore`.
Untuk distribusi ZIP gunakan allowlist file kode/katalog/dokumentasi; `.gitignore`
tidak otomatis melindungi arsip ZIP. Tidak ada perubahan token atau konfigurasi
produksi yang diperlukan untuk fitur pencarian ini.
