# Integrasi Android — AutoMate Jobs HTTP API V2

## Alamat dan menjalankan server

**Base URL: `http://127.0.0.1:8788/`**, termasuk garis miring akhir untuk Retrofit.
Implementasi Python stdlib, tanpa instalasi dependency server tambahan:

```sh
python -B http_api.py
```

Server selalu bind IPv4 `127.0.0.1`. Tidak ada opsi bind publik. Port 8787
dicadangkan untuk API lama, dan constructor V2 menolak port itu. Tidak ada
migrasi database, pembuatan sesi, penyimpanan token, atau pemanggilan bot.
Jika klien mengirim header `Authorization`, V2 hanya membaca identitas dari
`GET http://127.0.0.1:8787/auth/me` milik API lama (lihat bagian identitas).
Tanpa header tersebut, `/jobs` tetap publik seperti sebelumnya.

APK pada perangkat Android yang sama dengan Termux memakai alamat tersebut.
Latihan Psikotes memakai layanan terpisah **`http://127.0.0.1:8789/`**, bukan
rute 8788; kontraknya ada di **`PSYCHOTEST_API.md`**.
`127.0.0.1` adalah perangkat sendiri, bukan IP Termux yang dapat diakses melalui
LAN. Jika menguji dari komputer yang tersambung ADB ke perangkat Termux, gunakan
forward lokal berikut, bukan membuka server ke internet:

```sh
adb forward tcp:8788 tcp:8788
```

Klien Android memerlukan izin `INTERNET`. Untuk build pengembangan, kebijakan
Android cleartext HTTP harus mengizinkan localhost, misalnya melalui network
security config khusus localhost. Dokumentasi ini tidak mengubah atau membangun
ulang APK yang sudah ada.

## Endpoint

Semua endpoint di bawah menggunakan GET dan JSON UTF-8. Sukses V2 memiliki
`ok: true, api_version: 2`; error memiliki `ok: false, api_version: 2, error`.

| URL | Kegunaan / field utama |
| --- | --- |
| `http://127.0.0.1:8788/health` | Liveness, bind, uptime, jumlah sumber terkonfigurasi; tidak mengambil data upstream |
| `http://127.0.0.1:8788/jobs` | Lowongan LIVE + referensi perusahaan spontan, pagination, snapshot, status sumber, fallback |
| `http://127.0.0.1:8788/job-categories` | `job_categories` (14 kategori/69 referensi), `role_categories` (7 kelompok), `all_positions` |
| `http://127.0.0.1:8788/locations` | `locations` (lima wilayah dan subarea), `all_locations` |
| `http://127.0.0.1:8788/job-sources` | Enam kategori, 44 sumber registry, status konfigurasi, `company_catalog` lokal, tujuh metode lamaran |

Contoh health (field lain dihilangkan):

```json
{
  "ok": true,
  "api_version": 2,
  "status": "ok",
  "service": "automate-jobs-v2",
  "bind": {"host": "127.0.0.1", "port": 8788},
  "live_sources_configured": 17,
  "registered_sources": 44,
  "upstream_network_check": false,
  "company_catalog": {
    "companies": 135,
    "unique_emails": 142,
    "company_email_pairs": 142,
    "email_observations": 284,
    "verified_company_emails": 0,
    "imports": 1
  },
  "identity_bridge": "existing_android_api_read_only"
}
```

`company_catalog` bernilai `null` jika katalog lokal V2 tidak dimuat. Angka di
atas berasal dari impor workbook 29 Oktober 2026; kontak **belum diverifikasi
independen** dan email tidak dikirim oleh V2.

`live_configured` bukan pemeriksaan keterjangkauan setiap sumber. Status aktual
pengambilan tersedia pada `/jobs.source_status`; katalog/health selalu ringan.
Glints, Indeed, Jobstreet, dan LinkedIn berstatus `authorization_required`.

## Parameter GET /jobs

| Parameter | Default / aturan |
| --- | --- |
| `positions` | Semua jabatan; daftar judul/sinonim. Alias: `position`, `jabatan`, `q`, `query`, `keyword` |
| `locations` | Seluruh Jabodetabek. Alias: `location`, `lokasi`, `city` |
| `sources` | Semua kategori sumber. Alias: `source`, `source_category`, `source_categories` |
| `categories` | Kategori jabatan dari katalog atau OB/Helper/Driver/Kurir/Chef/Barista/Resepsionis. Alias: `category` |
| `source_ids` | Semua ID akun sumber; ID tersedia pada `/job-sources`. `company_catalog` hanya untuk referensi spontan. Alias: `source_id` |
| `max_age_days` | 30; bilangan 0–3650. `all` tanpa batas usia. Alias: `days` |
| `since` | Batas publikasi awal ISO 8601. Alias: `date_from` |
| `until` | Batas akhir ISO 8601, inklusif seluruh hari jika hanya tanggal. Alias: `date_to` |
| `page` | 1; bilangan mulai 1 |
| `page_size` | 20; 1–100. Alias: `limit` |
| `offset` | Alternatif pagination berbasis offset mulai 0; tidak boleh digabung dengan `page` |
| `snapshot_id` | ID dari respons pertama untuk melanjutkan snapshot yang sama |
| `format` | `v2`; `legacy` menghasilkan array. Alias: `compat`; nilai `v1`/`legacy_array` berarti `legacy` |

Daftar menerima parameter berulang, CSV, atau array JSON pada nilai query.
Contoh setara:

```text
/jobs?positions=Admin%20Gudang&positions=Data%20Analyst&locations=Jakarta&locations=Bekasi
/jobs?positions=Admin%20Gudang,Data%20Analyst&locations=Jakarta,Bekasi
```

Retrofit meng-encode daftar/Unicode secara otomatis. Untuk membangun URL manual,
encode nama kategori yang mengandung `&` dan offset zona waktu `+07:00`.

- OR dalam daftar jabatan, lokasi, dan sumber; AND antara kelompok filter.
- `categories` menambahkan referensi jabatan ke pilihan `positions` (gabungan OR).
- Enam ID kategori sumber: `company`, `bumn`, `government`, `healthcare`,
  `education`, `job_portal`. `source_ids` adalah ID perusahaan/portal, bukan
  kategori. ID khusus `company_catalog` memilih hanya peluang spontan lokal.
- Kosong/tidak diisi, `all`, `*`, atau label Semua dari katalog berarti semua
  pada kelompok tersebut. Semua Lokasi tetap terbatas Jabodetabek.
- `q` adalah alias pencocokan judul jabatan, bukan pencarian seluruh deskripsi.
- `since` tanpa `max_age_days` menggantikan default 30 hari. Jika keduanya diisi,
  digunakan irisan. `until` tanggal saja mencakup sampai 23:59:59.999999 UTC.
- Input di luar katalog wilayah, kategori sumber/ID tidak dikenal, parameter
  tidak dikenal, konflik alias, atau rentang tanggal terbalik menghasilkan 400
  sebelum pencarian upstream.
- Metadata API tidak menerima endpoint feed, credential, token, atau konfigurasi
  koneksi portal dari query HTTP. Portal yang belum berizin hanya memberi fallback.

## Identitas akun dan batas dua posisi

Header opsional:

```text
Authorization: Bearer <token sesi Android yang sudah ada>
```

Jika header ada, V2 memanggil `GET http://127.0.0.1:8787/auth/me` secara
read-only, tanpa login, refresh, perubahan akun, atau penyimpanan token. Respons
identitas wajib memiliki `id` bilangan bulat positif.

- Token tidak valid/kedaluwarsa: HTTP `401`, `error.code=IDENTITY_INVALID`.
- Layanan identitas tidak tersedia/merespons rusak: HTTP `503`,
  `error.code=IDENTITY_UNAVAILABLE`, dengan `Retry-After: 2`.
- Tanpa header: tetap `200`; `personalization.enabled=false`; batas akun tidak
  diterapkan karena tidak ada akun terverifikasi.
- Token valid: maksimum **dua posisi berbeda per perusahaan per akun**. State
  disimpan di SQLite lokal V2 dan berlaku lintas halaman, snapshot, serta query
  baru. Ini bukan pemotongan per halaman.
- Batas ini hanya menyaring tampilan/lowongan yang dapat dilamar. V2 tidak
  mengirim email, tidak membuat lamaran, dan tidak mengubah status Pro/akun.

Field respons tambahan saat katalog lokal dimuat:

```json
"personalization": {
  "enabled": true,
  "identity": {"kind": "verified_android_session", "account_id": 2},
  "limit_per_company": 2,
  "application_sending_enabled": false
}
```

Header `X-Personalized: 1` juga dikirim untuk sesi terverifikasi. ID akun hanya
dikembalikan kepada pemilik token tersebut.

## Penyimpanan lokal dan pembersihan alokasi uji

- Katalog perusahaan, observasi email, alokasi posisi, dan intent lamaran berada
  di `data/companies_v2.sqlite3`; file ini tidak dibaca sebagai feed lowongan.
- Batas tampilan memakai tabel `position_allocations` dengan kunci akun,
  perusahaan, dan posisi. Aturan batas lamaran disiapkan di `application_intents`
  dengan maksimum dua intent berbeda per perusahaan per akun; posisi yang sama
  bersifat idempoten. Aturan ini belum diekspos ke endpoint HTTP, tidak mengirim
  email, dan tidak membuat `position_allocations`.
- Alokasi uji lama `subject='android:2'`, `first_seen=1793289180` (10 baris,
  5 perusahaan) sudah dihapus persis; perusahaan, email, dan observasi tidak
  disentuh. Sisa `position_allocations=0`, `application_intents=0`.
- Sebelum pembersihan, ambil backup SQLite dan catat kunci yang akan dihapus.
  Hapus hanya batch dengan subject/timestamp yang sama, lalu verifikasi jumlah
  baris dan jumlah perusahaan/email. Jangan memakai `DELETE` tanpa filter.
- Bukti: `job_sources/v2_allocation_cleanup.json`, backup
  `backups/v2-final-20261029T220107Z/` dan `backups/v2-prerestart-20261029T220207Z/`.

## Peluang lamaran spontan

Selain lowongan LIVE, `/jobs` dapat memuat baris katalog perusahaan:

- `source_id="company_catalog"`, `result_type="spontaneous"`,
  `opportunity_label="Peluang Lamaran Spontan"`.
- `position_is_reference=true` dan `vacancy_verified=false`: jabatan adalah
  **referensi berdasarkan bidang usaha**, bukan pengumuman lowongan.
- `published_at=null`, `date_kind="not_applicable"`, dan tidak ada gaji/tanggal
  yang dikarang. Filter `max_age_days`/`since`/`until` hanya berlaku pada LIVE;
  lihat `summary.date_filter_applies_to`.
- `application_method="information_only"`, `application_email=null`, dan
  `email_sending_enabled=false`. Email perusahaan disimpan hanya untuk fitur
  lamaran Pro tahap berikutnya; pengiriman belum diaktifkan.
- `provenance` berisi sheet/baris workbook, lokasi, URL sumber, dan klaim
  verifikasi asli. Klaim “belum diverifikasi” tidak dinaikkan menjadi fakta.
- Setiap perusahaan memiliki lima referensi jabatan; hasil LOWongan LIVE dengan
  perusahaan+jabatan yang sama menggantikan referensi agar tidak duplikat.

## Respons pencarian dan pagination snapshot

Cuplikan berikut menggunakan **satu job LIVE asli** dari pengujian 29 Oktober
2026 13:09–13:10 UTC. Bentuknya menggambarkan halaman `page_size=1` dari snapshot
74 hasil LIVE waktu itu; field diagnostik dan baris katalog tidak ditampilkan.
Server yang memuat katalog juga menambahkan referensi spontan, sehingga `total`
saat ini lebih besar dan berubah sesuai filter. Angka bukan hasil hardcode.

```json
{
  "ok": true,
  "api_version": 2,
  "jobs": [
    {
      "title": "Desk Collection",
      "company": "Cermati.com",
      "location": "Jakarta Pusat, Gambir, Indonesia",
      "location_regions": ["Jakarta"],
      "position": "Semua Jabatan",
      "link": "https://jobs.smartrecruiters.com/Cermaticom/744000158506729-desk-collection?oga=true",
      "source": "Cermati Fintech Group",
      "source_id": "cermati",
      "source_category": "company",
      "source_link": "https://career.cermati.group/",
      "published_at": "2026-10-29T12:27:57.106000+00:00",
      "application_method": "portal_apply",
      "application_actions": [
        {
          "method": "portal_apply",
          "label": "Lamar di Portal",
          "url": "https://jobs.smartrecruiters.com/Cermaticom/744000158506729-desk-collection?oga=true"
        }
      ],
      "live": true
    }
  ],
  "count": 1,
  "total": 74,
  "total_found": 74,
  "pagination": {
    "page": 1,
    "page_size": 1,
    "offset": 0,
    "total": 74,
    "total_pages": 74,
    "has_more": true,
    "next_page": 2,
    "next_offset": 1
  },
  "partial": false,
  "empty_reason": null,
  "snapshot_truncated": false
}
```

Respons lengkap juga berisi:

- `cache`: `hit`, `snapshot_id`, `created_at`, `expires_at`.
- `source_status`: status aktual setiap sumber, jumlah hasil, complete/partial,
  error/izin, dan jumlah permintaan upstream.
- `fallback_links`: tautan resmi terpisah, **tidak dihitung sebagai lowongan**.
- `summary`: metrik mesin pencarian, filter efektif, sumber LIVE, wilayah,
  `portal_results`, `portal_live_integrations_verified`, `portal_data_complete`.
- `warnings`: kegagalan/hasil parsial sumber atau portal belum berizin.

Alur Android:

1. Ambil halaman pertama tanpa `snapshot_id`.
2. Simpan `cache.snapshot_id` dan filter aktif.
3. Untuk halaman berikutnya, kirim ID itu dan **filter yang sama**, beserta
   `pagination.next_page` atau `next_offset` (pilih salah satu model).
4. Jika filter berubah, buang snapshot dan mulai halaman 1.
5. Jika 410, snapshot kedaluwarsa/tereviksi: mulai ulang dan ganti daftar, jangan
   mencampur halaman dari dua snapshot. Konflik filter dengan snapshot adalah 409.

Pagination dilakukan setelah normalisasi/deduplikasi. `total` adalah jumlah
job valid dalam snapshot; `count` jumlah halaman saat ini. `total_found` adalah
jumlah teramati mesin sebelum cap snapshot HTTP. Maksimum snapshot 1.000 hasil;
`snapshot_truncated: true` menandakan lebih banyak hasil teramati. Batas
detail/halaman/permintaan upstream juga dapat membuat hasil `partial`.

Halaman di luar akhir menghasilkan 200 dengan `jobs: []`, `count: 0`, dan
`has_more: false`, bukan mengambil dataset baru. Jika tidak ada job, periksa
`empty_reason`: `no_matching_jobs`, `authorization_required`, `no_live_connector`,
atau `sources_unavailable`; jangan menyimpulkan portal tidak memiliki lowongan
dari kegagalan/izin yang belum tersedia.

## Lapisan kompatibilitas Android lama

```text
/jobs?position=Admin%20Warehouse&location=Bekasi&source=company&limit=12&offset=0&format=legacy
```

Mode `legacy` mengembalikan **JSON array** berisi job. Field lama dipertahankan:
`title`, `company`, `location`, `position`, `link`, `source`, `source_link`,
`updated`, `remote`, `expiry_ts`, `match_label`. Alias tambahan:

| Alias kompatibilitas | Asal |
| --- | --- |
| `url` | `link` |
| `job_url` | URL asli jika tersedia, selain itu sumber resmi |
| `job_title` | `title` |
| `company_name` | `company` |
| `job_location` | `location` |
| `date_posted` | `published_at`, bukan tanggal scraping |
| `apply_url` | `application_url`, boleh null |
| `action_label`, `action_url` | Tindakan yang benar-benar tersedia |
| `can_apply` | false untuk informasi/PDF saja |

Header mode array: `X-Search-Snapshot`, `X-Total-Count`, `X-Has-More`,
`X-Next-Offset`, `X-Cache`, `X-API-Version`. Gunakan Retrofit `Response<List<Job>>`
jika perlu membaca header. Error tetap objek JSON dengan HTTP non-2xx.

Folder proyek tidak berisi kode APK atau rute API 8787, sehingga kompatibilitas
yang teruji adalah kontrak field, alias parameter, dan bentuk JSON tersebut.
APK yang meng-hardcode port 8787 tetap membutuhkan opsi base URL ke 8788 atau
perubahan klien terpisah; server ini tidak mengalihkan/mengubah port 8787.
Tidak ada pengujian UI APK end-to-end atau pembangunan ulang APK pada tugas ini.

## Tujuh metode lamaran

`portal_apply`, `company_form`, `email_hr`, `recruitment_portal`,
`government_registration`, `announcement_pdf`, `information_only` tetap tersedia.
Render tombol langsung dari `application_actions`, bukan membuat tombol
“Lamar Sekarang” hanya karena ada `link`. URL Apply bisa null. Data/sumber tidak
menyediakan bukti pendaftaran berarti tombol **Lihat Sumber Resmi**. Membuka
tindakan adalah interaksi manual pengguna; HTTP API tidak melakukan auto-apply.

## Timeout, cache, rate limit, dan kesalahan

- Request upstream tetap memakai mesin V2: timeout 10 detik, pacing per host,
  cache memori upstream 15 menit, maksimum 100 permintaan/180 detik per pencarian.
  Pacing default 1,0 detik per host; layanan V2 saat ini memakai 0,6 detik melalui
  `AUTOMATE_JOBS_V2_UPSTREAM_INTERVAL` (dibatasi 0,25–2,0 detik). Turunkan pacing
  hanya setelah verifikasi hasil dan status sumber tetap sama.
- Snapshot HTTP cache LRU: 16 query, TTL 5 menit (hasil parsial maksimum 30 detik).
  Cache juga dibatasi tanggal kedaluwarsa yang diketahui. Cache pencarian hanya
  berada di memori. Katalog perusahaan, alokasi posisi, dan intent lamaran memakai
  SQLite lokal `data/companies_v2.sqlite3`. Halaman dan format respons berbeda
  berbagi snapshot query yang sama.
- Satu fetch aktif; request query yang sama bergabung (*single-flight*). Query
  baru yang berbeda saat sibuk mendapat 503 `SEARCH_BUSY`, bukan antrean tak terbatas.
- Maksimum 120 request HTTP/menit per proses dan 6 query baru/menit. Halaman cache
  tidak memakan kuota query baru, tetapi tetap dihitung sebagai request HTTP.
- Maksimum 16 handler HTTP, timeout pembacaan socket 10 detik, URL maksimum
  8 KiB, daftar maksimum 30 pilihan per kelompok.
- HTTP menunggu fetch maksimum 190 detik. Timeout menghasilkan 504; kerja yang
  masih berlangsung tidak diduplikasi oleh retry identik.
- Kegagalan satu sumber yang ditangani mesin tetap menghasilkan 200 dengan job
  dari sumber lain, `partial`, `source_status`, dan `warnings`. Kegagalan fatal
  mesin menjadi 502 tanpa stack trace/credential dalam respons.

Set **read timeout Android 210 detik** untuk pencarian dingin. Pengujian kode
baru membutuhkan **60,2 detik** (baseline 92,7 detik; historis 104 detik). Waktu
dapat berubah menurut jaringan, jadi pertahankan timeout 210 detik. Katalog/health
cepat dan pagination menggunakan cache. Hindari request pada setiap ketikan;
hormati `Retry-After`.

| HTTP | Kode penting |
| --- | --- |
| 400 | `INVALID_PARAMETER` |
| 404 | `NOT_FOUND` |
| 405 | `METHOD_NOT_ALLOWED` (GET saja) |
| 409 | `SNAPSHOT_QUERY_MISMATCH` |
| 410 | `SNAPSHOT_EXPIRED` |
| 414 | `URI_TOO_LONG` |
| 429 | `RATE_LIMITED`, dengan `Retry-After` |
| 502 | `SEARCH_FAILED` |
| 503 | `SEARCH_BUSY` / `SERVER_BUSY`, dengan `Retry-After` |
| 504 | `SEARCH_TIMEOUT`, dengan `Retry-After` |

```json
{"ok":false,"api_version":2,"error":{"code":"INVALID_PARAMETER","message":"Lokasi di luar katalog Jabodetabek: Bandung"}}
```

## Contoh Retrofit/Kotlin

```kotlin
interface JobsV2Api {
    @GET("health") suspend fun health(): HealthResponse

    @GET("jobs")
    suspend fun jobs(
        @Query("positions") positions: List<String>? = null,
        @Query("locations") locations: List<String>? = null,
        @Query("sources") sources: List<String>? = null,
        @Query("categories") categories: List<String>? = null,
        @Query("source_ids") sourceIds: List<String>? = null,
        @Query("max_age_days") maxAgeDays: Int = 30,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("snapshot_id") snapshotId: String? = null
    ): Response<JobsResponse>
}

// Retrofit + Gson contoh; DTO lain mengikuti JSON endpoint katalog/health.
data class ApplicationAction(val method: String, val label: String, val url: String)
data class Job(
    val title: String, val company: String, val location: String, val link: String,
    @SerializedName("source_id") val sourceId: String,
    @SerializedName("application_method") val applicationMethod: String,
    @SerializedName("application_actions") val actions: List<ApplicationAction>,
    @SerializedName("application_url") val applicationUrl: String?,
    @SerializedName("published_at") val publishedAt: String?
)
data class Pagination(
    val page: Int?, @SerializedName("page_size") val pageSize: Int,
    val total: Int, @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("next_page") val nextPage: Int?
)
data class SearchCache(@SerializedName("snapshot_id") val snapshotId: String, val hit: Boolean)
data class JobsResponse(
    val ok: Boolean, val jobs: List<Job>, val count: Int, val total: Int,
    val partial: Boolean, val pagination: Pagination, val cache: SearchCache
)

val httpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(210, TimeUnit.SECONDS)
    .build()
val retrofit = Retrofit.Builder()
    .baseUrl("http://127.0.0.1:8788/")
    .client(httpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

## Bukti pengujian

**167 tes otomatis lulus**. Tes memakai database/transport sementara dan tidak
mengklaim hasil sintetik sebagai lowongan LIVE.

Pengujian final 29 Oktober 2026 22:03–22:08 UTC pada kode baru: **28/28
pemeriksaan HTTP lulus**, **74 lowongan LIVE**, **675 referensi spontan**
(**749 total**), **17 sumber API berhasil**, dan 94 permintaan upstream.
Wilayah: Jakarta 64, Bogor 2, Depok 0, Tangerang 1, Bekasi 7. Empat portal tetap
belum berizin dan menghasilkan 0 job. Pencarian dingin **60,2 detik** (baseline
92,7 detik, -35,1%); seluruh 8 halaman snapshot memakai cache dan 749
`candidate_id` unik tanpa duplikasi.

Batas dua posisi/lamaran diuji pada server ephemeral dengan identitas sintetis:
maksimum 2 posisi per perusahaan lintas halaman/query, dan intent ketiga ditolak.
**Uji dengan token Android nyata belum dilakukan**; jangan anggap identitas
sintetis sebagai bukti sesi produksi.

Bukti final: `job_sources/http_api_verification_v2_final.json`,
`job_sources/v2_final_live_before.json`, `v2_final_live_after.json`,
`v2_final_pagination_verification.json`, dan
`v2_final_limit_verification.json`. Bukti 20/20 historis tetap di
`job_sources/http_api_verification.json`. Tidak ada fixture yang dibaca server.

```sh
PYTHONDONTWRITEBYTECODE=1 python -B -m unittest discover -q

# Server harus sudah berjalan; simpan bukti baru, tidak menimpa bukti lama:
python -B verify_http_api.py --output job_sources/http_check_baru.json
```
