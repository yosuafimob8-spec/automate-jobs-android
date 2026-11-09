package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.ScreenPageGradient
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.ScreenText
import com.example.automatesmartbot.ui.theme.ScreenMuted

private val aboutBlue = Color(0xFF2563A6)

@Composable
private fun AboutPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
        shape = RoundedCornerShape(17.dp)
    ) {
        Column(
            Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content
        )
    }
}

@Composable
private fun AboutMenu(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = ScreenText)
                Text(subtitle, fontSize = 12.sp, color = ScreenMuted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = aboutBlue)
        }
    }
}

@Composable
fun AboutKarirMateHome(
    onOpenDescription: () -> Unit,
    onOpenDevelopers: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val context = LocalContext.current
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
            .versionName ?: "-"
    } catch (_: Exception) { "-" }

    Column(
        Modifier.fillMaxSize()
            .background(ScreenPageGradient)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AboutPanel {
            Text(
                "KarirMate",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = aboutBlue,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "Teman Perjalanan Kariermu",
                color = ScreenText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "Versi Aplikasi: $version",
                color = ScreenMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        AboutMenu(
            "Tentang KarirMate",
            "Deskripsi, tujuan, dan manfaat aplikasi",
            onOpenDescription
        )
        AboutMenu(
            "Pengembang",
            "Ilham Ramdhani S & Tanti Kurnia",
            onOpenDevelopers
        )
        AboutMenu(
            "Privasi & Ketentuan",
            "Informasi penggunaan dan perlindungan data",
            onOpenPrivacy
        )
        AboutMenu(
            "Bantuan & Kontak",
            "Laporan kendala, kritik, dan saran",
            onOpenSupport
        )
        Text(
            "Made with love in Indonesia",
            color = ScreenMuted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AboutKarirMateDetail(section: String) {
    val title: String
    val paragraphs: List<String>

    when (section) {
        "description" -> {
            title = "Tentang KarirMate"
            paragraphs = listOf(
                "KarirMate — Teman Perjalanan Kariermu.",
                "KarirMate adalah aplikasi pendamping karier yang membantu pencari kerja mempersiapkan diri, meningkatkan keterampilan, dan mengelola perjalanan karier dalam satu platform.",
                "KarirMate menghadirkan pembuatan CV, surat lamaran, psikotes, simulasi wawancara AI, pelatihan kerja, dan informasi dunia kerja.",
                "Kami percaya setiap orang berhak mendapatkan kesempatan untuk belajar, berkembang, dan membangun masa depan karier yang lebih baik.",
                "KarirMate — Siapkan Diri, Raih Karier Impian."
            )
        }
        "developers" -> {
            title = "Pengembang KarirMate"
            paragraphs = listOf(
                "Ilham Ramdhani S",
                "Tanti Kurnia",
                "KarirMate dikembangkan secara independen untuk menghadirkan solusi digital yang membantu masyarakat Indonesia mempersiapkan diri menghadapi dunia kerja.",
                "Kami berkomitmen mengembangkan aplikasi yang inovatif, praktis, mudah digunakan, dan bermanfaat.",
                "Made in Indonesia"
            )
        }
        "privacy" -> {
            title = "Privasi & Ketentuan"
            paragraphs = listOf(
                "Privasi Pengguna",
                "KarirMate menggunakan informasi yang diberikan pengguna untuk mendukung fitur akun, profil, CV, persiapan karier, dan pelatihan. Pengolahan data juga dapat melibatkan layanan backend yang digunakan oleh aplikasi.",
                "Jangan memasukkan informasi rahasia atau sensitif yang tidak diperlukan oleh fitur.",
                "Ketentuan Penggunaan",
                "KarirMate merupakan aplikasi pendukung persiapan karier, bukan lembaga penyalur tenaga kerja resmi.",
                "Hasil psikotes, simulasi wawancara AI, evaluasi pelatihan, dan sertifikat internal merupakan sarana pembelajaran, bukan jaminan diterima bekerja atau sertifikasi kompetensi resmi.",
                "Ringkasan ini bukan dokumen kebijakan privasi lengkap. Dokumen resmi perlu disediakan sebelum publikasi aplikasi."
            )
        }
        "support" -> {
            title = "Bantuan & Kontak"
            paragraphs = listOf(
                "Butuh Bantuan?",
                "Kami menerima pertanyaan, laporan masalah, kritik, dan saran untuk meningkatkan kualitas KarirMate.",
                "Bantuan meliputi kendala fitur aplikasi, pertanyaan penggunaan, dan informasi pembaruan.",
                "Email bantuan resmi: belum tersedia.",
                "Saluran bantuan resmi akan ditambahkan setelah ditetapkan."
            )
        }
        else -> {
            title = "Tentang KarirMate"
            paragraphs = listOf("Informasi tidak tersedia.")
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(ScreenPageGradient)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AboutPanel {
            Text(
                title,
                color = aboutBlue,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            paragraphs.forEach { text ->
                Text(
                    text,
                    color = ScreenText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
