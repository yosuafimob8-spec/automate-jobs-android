# Kontrak data ketenagakerjaan dashboard KarirMate

Endpoint Android: GET `{TRAINING_BASE_URL}/public/labor-stats` (HTTPS).

Status: klien Android sudah mengonsumsi kontrak ini. **Endpoint server dan sinkronisasi BPS belum diimplementasikan.** Jangan menyajikan data contoh sebagai data terbaru.

## JSON yang wajib dikembalikan

```json
{
  "source": "BPS",
  "period": "Periode referensi masing-masing indikator, bukan tanggal fetch",
  "metrics": {
    "unemployment": "angka + satuan",
    "tpt": "persentase",
    "poverty": "angka + satuan",
    "povertyRate": "persentase",
    "workforce": "angka + satuan",
    "employed": "angka + satuan",
    "wages": "rupiah per bulan"
  },
  "unemploymentSeries": [{"period": "YYYY-MM", "value": 0.0}],
  "povertySeries": [{"period": "YYYY-MM", "value": 0.0}]
}
```

## Ketentuan sinkronisasi

1. Registrasi WebAPI BPS resmi: https://webapi.bps.go.id/developer/ . API key hanya di server.
2. Konfigurasi endpoint/ID tabel resmi BPS untuk masing-masing indikator. Jangan menebak ID variabel atau nilai statistik.
3. Simpan metadata sumber, satuan, periode rilis, dan timestamp pengambilan. Seri grafik harus dari observasi asli yang konsisten definisi/satuannya.
4. Cache respons server; refresh maksimal satu kali sehari dan hormati rate limit BPS. Bila BPS gagal, tampilkan data cache beserta tanggal/indikasi bahwa data mungkin lama.
5. Perbarui indikator secara atomik setelah validasi; jangan gabungkan periode berbeda menjadi satu periode yang menyesatkan.
6. Tambahkan atribusi wajib: \"Layanan ini menggunakan API Badan Pusat Statistik (BPS).\" saat WebAPI dipakai.
7. Sebelum rilis, uji respons riil server, kosong/offline, grafik dua titik dan banyak titik, dan verifikasi hasil terhadap publikasi BPS.

Referensi ketentuan: https://webapi.bps.go.id/developer/
