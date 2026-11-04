# Materi Audio dan audit ulang Materi Visual

Perubahan ini sudah ada lokal ketika pekerjaan STEP 3–6 dimulai, lalu dipertahankan dan diuji bersama regresi akhir. Tidak ada push atau build APK baru.

## Materi Audio

`TrainingNarration.kt` sebelumnya memakai kartu putih tetap, teks gelap tetap, border `#FFDEC9` yang pucat, dan tombol putar oranye dengan teks putih. Tombol Berhenti tidak mempunyai pasangan background aktif/nonaktif eksplisit. Sekarang kartu mengikuti surface tema; tombol Dengarkan/Putar Ulang memakai oranye merek dengan tinta gelap; Berhenti mempunyai foreground, background opaque, dan border tersendiri untuk kedua status.

Lima test `TrainingNarrationTest` menjalankan komponen asli dengan ShadowTextToSpeech: belum siap/gagal inisialisasi, siap namun belum memutar, Dengarkan, Putar Ulang, Berhenti, pergantian tema saat memutar, serta stop/shutdown ketika komponen ditutup. Warna teks aktual diperiksa dari TextLayoutResult. Pasangan teks minimal 4,5:1; border Berhenti minimal 3:1 terhadap background tombol maupun kartu. Tidak ada perubahan pada naskah narasi, QUEUE_FLUSH, locale Indonesia, atau callback stop yang sudah tersedia.

Output suara, engine TTS perangkat, dan inspeksi visual sentuhan nyata belum diuji pada Android. Test bukan bukti suara di perangkat berhasil.

## Bug 11: navy belum membuktikan pemulihan desain lengkap

Tiga bagian berbeda tampil berurutan pada halaman modul:

| Bagian | Sebelum redesign V78 | V78 | Source saat ini |
|---|---|---|---|
| Ilustrasi modul `TrainingLessonVisual` | Kartu mint `#E7F7F3`, ikon teal, judul navy (`d81f29c`, sebelum `403d4e5`) | Kartu putih, ikon oranye, border hangat | Masih desain V78; bukan area yang diubah oleh warna TrainingVideo |
| Slide pengganti MP4 pada `TrainingVideo`, hanya saat resource tidak ditemukan | Navy `#152D43`, label amber tanpa badge, teks putih, dua tombol outlined (`958131f`, `b72a32d`) | Hampir hitam `#0F172A`, badge cream, counter amber, tombol berikutnya filled (`403d4e5`) | Navy dipulihkan oleh `07d6ed8`, tetapi badge/border/bentuk tombol/counter tetap mengikuti redesign dan perbaikan kontras |
| Isi MP4 | Generator `3f7ee3c` memakai navy `#152D43`, panel putih transparan 10%, judul amber dan teks putih | Generator sama | Generator dan jalur pemutar sama; perubahan fallback tidak mengubah MP4 |

Audit sebelumnya hanya memulihkan warna fallback. Itu **bukan pemulihan keseluruhan desain sebelum V78**, dan tidak membuktikan bahwa area yang dilaporkan pengguna sudah benar.

Audit baru membaca artefak V78 yang SUDAH ADA dari run canonical #78 (`41770736672`, sumber `1fe061c4f8693dee02131e8ed5d8eb743f903cf5`). ZIP diverifikasi dengan SHA-256 `63ab7b98147eea63d660c574ce8efe1428d6a3e21de2ad957153a34560dfaa5d`. APK dibaca di memori tanpa dibangun atau dipasang. Ada 20 MP4; satu sampel `training_administration_0.mp4` diekstrak: H.264, 640×360, 9 detik. Frame pertama memperlihatkan navy dan panel transparan, bukan latar hitam polos. Lihat [frame MP4 V78](verification/v78-first-frame.png) dan [metadata inspeksi](verification/artifact-inspection.json).

Kesimpulan acuan: mint adalah desain historis ilustrasi modul; navy adalah desain historis isi media/fallback. Keduanya tidak boleh disamakan. Jika pengguna melihat hitam pada pemutar MP4, penggantian warna branch `id == 0` tidak akan memengaruhi resource MP4 yang ditemukan. Perilaku pemutar saat belum mulai/buffering/error dan bentuk area yang pengguna maksud masih perlu diverifikasi di perangkat. Satu sampel MP4 juga memperlihatkan deskripsi terpotong di kanan; media tidak diubah dalam tugas ini.

`TrainingVideo.kt`, `TrainingVisuals.kt`, generator video, dan konten pelatihan tidak diubah lagi pada revisi STEP 3–6. **Bug 11 tetap BELUM FINAL secara visual.** Tidak ada klaim bahwa navy saja sudah memenuhi desain yang disepakati pengguna.
