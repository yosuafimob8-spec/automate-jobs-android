package com.example.automatesmartbot.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.ScreenPageGradient
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.ScreenText
import com.example.automatesmartbot.ui.theme.ScreenMuted

private val ocean = Color(0xFF2563A6)
private val mint = Color(0xFF55C6A9)
private val ice = Color(0xFFEAF5F9)
private val border = Color(0xFFC6DFE8)
private val ink = Color(0xFF243547)

@Composable
private fun AboutCanvas(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenPageGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 19.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun AboutCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(19.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content
        )
    }
}

@Composable
private fun IconMark(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(46.dp).background(ice, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = ocean, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SectionIntro(icon: ImageVector, eyebrow: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconMark(icon)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(eyebrow.uppercase(), color = ocean, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(title, color = ScreenText, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)
            Text(subtitle, color = ScreenMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun Paragraph(value: String) {
    Text(value, color = ScreenText, fontSize = 14.sp, lineHeight = 22.sp)
}

@Composable
private fun SectionHeading(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Icon(icon, contentDescription = null, tint = ocean, modifier = Modifier.size(19.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ScreenText)
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, border),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            IconMark(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = ScreenText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = ScreenMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Buka $title", tint = ocean)
        }
    }
}

@Composable
private fun AboutFooter() {
    Text(
        "Dikembangkan dengan bangga di Indonesia",
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        color = ScreenMuted
    )
}

@Composable
fun AboutKarirMateHome(
    onOpenDescription: () -> Unit,
    onOpenDevelopers: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-"
        }.getOrDefault("-")
    }

    AboutCanvas {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ocean)
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(ocean, Color(0xFF1185A0))))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(9.dp)) {
                    Text(
                        "PROFIL APLIKASI",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
                Text("KarirMate", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text("Teman Perjalanan Kariermu", color = Color.White, fontSize = 15.sp)
                HorizontalDivider(color = Color.White.copy(alpha = 0.24f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Versi aplikasi $version", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Text("Jelajahi informasi", color = ScreenText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Kenali aplikasi, tim, dan layanan yang tersedia.", color = ScreenMuted, fontSize = 12.sp)
        MenuCard(Icons.Default.Info, "Tentang KarirMate", "Profil aplikasi, tujuan, dan manfaat", onOpenDescription)
        MenuCard(Icons.Default.People, "Pengembang", "Tim pengembang dan peran utama", onOpenDevelopers)
        MenuCard(Icons.Default.Security, "Privasi & Ketentuan", "Informasi penggunaan dan perlindungan data", onOpenPrivacy)
        MenuCard(Icons.Default.HelpOutline, "Bantuan & Kontak", "Pusat bantuan, kritik, dan saran", onOpenSupport)
        AboutFooter()
    }
}

@Composable
private fun AboutDescriptionPage() {
    AboutCanvas {
        SectionIntro(Icons.Default.Info, "Mengenal aplikasi", "Tentang KarirMate", "Satu langkah lebih dekat menuju karier impian")
        AboutCard {
            Text("Teman Perjalanan Kariermu", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ocean)
            Paragraph("KarirMate adalah aplikasi pendamping karier yang dirancang untuk membantu pengguna mempersiapkan diri menghadapi dunia kerja secara lebih terarah, praktis, dan modern.")
            Paragraph("Melalui satu platform, pengguna dapat menyiapkan dokumen profesional, berlatih menghadapi seleksi, serta meningkatkan kesiapan kerja.")
        }
        Text("Apa yang dapat kamu lakukan?", fontSize = 18.sp, color = ScreenText, fontWeight = FontWeight.Bold)
        AboutCard {
            SectionHeading("Dokumen profesional", Icons.Default.Description)
            Paragraph("Buat CV dan surat lamaran untuk mendukung proses pencarian kerja.")
            HorizontalDivider(color = border)
            SectionHeading("Persiapan seleksi", Icons.Default.CheckCircle)
            Paragraph("Latihan psikotes dan simulasi wawancara AI untuk meningkatkan kepercayaan diri.")
            HorizontalDivider(color = border)
            SectionHeading("Pengembangan diri", Icons.Default.School)
            Paragraph("Pelatihan kerja dan informasi dunia kerja yang membantu pengguna terus belajar.")
        }
        AboutCard {
            SectionHeading("Komitmen kami", Icons.Default.FavoriteBorder)
            Paragraph("Kami percaya setiap orang berhak memiliki akses ke alat bantu karier yang relevan, mudah digunakan, dan bermanfaat.")
            Text("Siapkan Diri, Raih Karier Impian.", color = ocean, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        AboutFooter()
    }
}

@Composable
private fun DeveloperProfile(name: String, role: String, responsibility: String) {
    AboutCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(54.dp).background(ice, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ocean)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(name, color = ScreenText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(8.dp), color = ice) {
                    Text(
                        role,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = ocean,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        HorizontalDivider(color = border)
        Paragraph(responsibility)
    }
}

@Composable
private fun AboutDevelopersPage() {
    AboutCanvas {
        SectionIntro(Icons.Default.People, "Tim di balik KarirMate", "Pengembang", "Dibangun secara independen di Indonesia")
        AboutCard {
            Paragraph("KarirMate dikembangkan secara independen untuk menghadirkan solusi digital yang membantu masyarakat Indonesia mempersiapkan diri menghadapi dunia kerja.")
        }
        DeveloperProfile(
            "Ilham Ramdhani S",
            "Founder & Product Director",
            "Memimpin visi produk, strategi, perencanaan fitur, pengalaman pengguna, dan arah pengembangan KarirMate."
        )
        DeveloperProfile(
            "Tanti Kurnia",
            "Co-Founder & Operations Director",
            "Mendukung operasional, koordinasi pengembangan, konten pendukung, dan peningkatan kualitas layanan KarirMate."
        )
        AboutCard {
            SectionHeading("Visi bersama", Icons.Default.StarOutline)
            Paragraph("Mengembangkan aplikasi yang inovatif, praktis, mudah digunakan, dan memberi manfaat nyata bagi pengguna.")
        }
        AboutFooter()
    }
}

@Composable
private fun PolicySection(title: String, icon: ImageVector, text: String) {
    AboutCard {
        SectionHeading(title, icon)
        Paragraph(text)
    }
}

@Composable
private fun AboutPrivacyPage() {
    AboutCanvas {
        SectionIntro(Icons.Default.Security, "Informasi penting", "Privasi & Ketentuan", "Pahami penggunaan data dan batasan layanan")
        PolicySection(
            "Penggunaan informasi",
            Icons.Default.LockOutline,
            "KarirMate menggunakan informasi yang diberikan pengguna untuk mendukung fitur akun, profil, CV, surat lamaran, psikotes, pelatihan, dan persiapan karier. Fitur tertentu dapat memerlukan pemrosesan melalui backend atau layanan pihak ketiga."
        )
        PolicySection(
            "Keamanan dan kendali pengguna",
            Icons.Default.VerifiedUser,
            "Hindari memasukkan informasi rahasia yang tidak diperlukan. Tinjau kembali dokumen dan informasi sebelum membagikannya melalui layanan atau aplikasi lain."
        )
        PolicySection(
            "Ketentuan layanan",
            Icons.Default.Description,
            "KarirMate merupakan aplikasi pendamping persiapan karier dan pengembangan diri, bukan lembaga penyalur tenaga kerja resmi atau lembaga sertifikasi kompetensi."
        )
        PolicySection(
            "Batasan hasil",
            Icons.Default.Info,
            "Hasil psikotes, simulasi wawancara AI, evaluasi pelatihan, dan sertifikat internal digunakan sebagai sarana pembelajaran, bukan jaminan diterima bekerja atau sertifikasi kompetensi resmi."
        )
        AboutCard {
            SectionHeading("Catatan sebelum publikasi", Icons.Default.WarningAmber)
            Paragraph("Informasi di halaman ini adalah ringkasan, bukan kebijakan privasi lengkap. Dokumen kebijakan resmi dan saluran kontak harus disiapkan sebelum publikasi aplikasi.")
        }
        AboutFooter()
    }
}

@Composable
private fun SupportFaq(question: String, answer: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, border)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(question, modifier = Modifier.weight(1f), color = ScreenText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Tutup jawaban" else "Lihat jawaban",
                    tint = ocean
                )
            }
            if (expanded) {
                HorizontalDivider(color = border)
                Paragraph(answer)
            }
        }
    }
}

@Composable
private fun AboutSupportPage() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var report by rememberSaveable { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }

    AboutCanvas {
        SectionIntro(Icons.Default.HelpOutline, "Pusat bantuan", "Bantuan & Kontak", "Saran, pertanyaan, dan laporan kendala")
        AboutCard {
            SectionHeading("Ada yang bisa kami bantu?", Icons.Default.SupportAgent)
            Paragraph("Sampaikan pertanyaan atau detail masalah yang kamu alami. Sertakan nama fitur dan langkah terjadinya kendala, tanpa membagikan kata sandi atau token akun.")
        }
        Text("Pertanyaan umum", color = ScreenText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        SupportFaq("Bagaimana jika fitur tidak merespons?", "Coba buka ulang fitur, periksa koneksi yang dibutuhkan, dan gunakan versi aplikasi terbaru. Jika masalah berlanjut, tulis detail kendalanya di bawah.")
        SupportFaq("Apakah hasil pelatihan merupakan sertifikat resmi?", "Hasil pelatihan dan sertifikat internal KarirMate merupakan sarana pembelajaran, bukan sertifikasi kompetensi resmi.")
        SupportFaq("Bagaimana melaporkan kesalahan aplikasi?", "Isi formulir laporan singkat berikut, lalu salin atau bagikan melalui aplikasi pilihanmu. Saluran dukungan resmi belum tersedia.")

        AboutCard {
            SectionHeading("Tulis laporan atau saran", Icons.Default.Edit)
            OutlinedTextField(
                value = report,
                onValueChange = { report = it; copied = false; shareFailed = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Detail kendala atau masukan") },
                placeholder = { Text("Contoh: Tombol Mulai Interview tidak merespons...") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ocean,
                    focusedTextColor = ink,
                    unfocusedTextColor = ink,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan KarirMate")
                        putExtra(Intent.EXTRA_TEXT, "Laporan KarirMate:\n${report.trim()}")
                    }
                    shareFailed = runCatching {
                        context.startActivity(Intent.createChooser(intent, "Bagikan laporan melalui"))
                    }.isFailure
                },
                enabled = report.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ocean),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bagikan Laporan")
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(report.trim())); copied = true },
                enabled = report.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (copied) "Laporan Disalin" else "Salin Laporan")
            }
            if (shareFailed) Text("Aplikasi untuk membagikan laporan tidak ditemukan.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            Text("Laporan tidak dikirim otomatis ke KarirMate. Pilih sendiri tujuan berbagi; saluran bantuan resmi belum tersedia.", color = ScreenMuted, fontSize = 11.sp, lineHeight = 17.sp)
        }
        AboutCard {
            SectionHeading("Kontak resmi", Icons.Default.Email)
            Paragraph("Email dan saluran bantuan resmi akan ditampilkan di sini setelah tersedia. Kami tidak menampilkan alamat kontak yang belum terverifikasi.")
        }
        AboutFooter()
    }
}

@Composable
fun AboutKarirMateDetail(section: String) {
    when (section) {
        "description" -> AboutDescriptionPage()
        "developers" -> AboutDevelopersPage()
        "privacy" -> AboutPrivacyPage()
        "support" -> AboutSupportPage()
        else -> AboutCanvas { AboutCard { Paragraph("Informasi belum tersedia.") } }
    }
}
