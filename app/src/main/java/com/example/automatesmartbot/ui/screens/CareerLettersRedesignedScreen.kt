package com.example.automatesmartbot.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.career.CareerLetters
import com.example.automatesmartbot.data.cv.CvRenderer
import com.example.automatesmartbot.ui.components.CvDocumentPreview
import com.example.automatesmartbot.ui.components.PdfExportButton
import com.example.automatesmartbot.ui.theme.ScreenPageGradient
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.ScreenText
import com.example.automatesmartbot.ui.theme.ScreenMuted
import com.example.automatesmartbot.ui.theme.karirInputColors
import com.example.automatesmartbot.ui.theme.screenCanvas
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val letterBlue = Color(0xFF065DB2)
private val letterBorder = Color(0xFFCEDEEB)

/** Update 7: presentation only; reuses the original resignation PDF and CV Builder flows. */
@Composable
internal fun CareerLettersRedesignedScreen(onApplicationLetter: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("career_resignation", 0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var company by rememberSaveable { mutableStateOf(prefs.getString("company", "").orEmpty()) }
    var position by rememberSaveable { mutableStateOf(prefs.getString("position", "").orEmpty()) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var lastDay by rememberSaveable { mutableStateOf(prefs.getString("last_day", "").orEmpty()) }
    var reason by rememberSaveable { mutableStateOf(prefs.getString("reason", "").orEmpty()) }
    var content by rememberSaveable { mutableStateOf(prefs.getString("content", "").orEmpty()) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var dateToPick by remember { mutableStateOf<String?>(null) }

    // Preserve the existing local draft storage contract and retain optional fields.
    LaunchedEffect(name, company, position, lastDay, reason, content) {
        prefs.edit().putString("name", name).putString("company", company)
            .putString("position", position).putString("last_day", lastDay)
            .putString("reason", reason).putString("content", content).apply()
    }
    if (dateToPick != null) {
        CareerLetterDateDialog(
            initial = if (dateToPick == "letter") date else lastDay,
            onDismiss = { dateToPick = null },
            onSelected = { chosen ->
                if (dateToPick == "letter") date = chosen else lastDay = chosen
                dateToPick = null
            }
        )
    }
    Column(
        Modifier.fillMaxSize().background(ScreenPageGradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        HorizontalDivider(color = screenCanvas(Color(0xFFC9E7F5)), thickness = 1.dp)
        CareerLetterIntroCard()
        CareerLetterCreateCard(onApplicationLetter)
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp),
            border = BorderStroke(1.dp, screenCanvas(letterBorder)),
            colors = CardDefaults.cardColors(containerColor = ScreenSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null,
                        modifier = Modifier.size(28.dp), tint = letterBlue)
                    Text("Surat Pengunduran Diri (Resign)", color = ScreenText,
                        fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(1.dp))
                CareerLetterField("Nama lengkap", name, { name = it }, "Masukkan nama lengkap", Icons.Default.Person)
                CareerLetterField("Perusahaan saat ini", company, { company = it }, "Masukkan nama perusahaan", Icons.Default.Business)
                CareerLetterField("Jabatan saat ini", position, { position = it }, "Masukkan jabatan saat ini", Icons.Default.Work)
                CareerLetterField("Tanggal surat (YYYY-MM-DD)", date, { date = it }, "YYYY-MM-DD",
                    Icons.Default.CalendarMonth, onChooseDate = { dateToPick = "letter" })
                CareerLetterField("Usulan hari kerja terakhir (YYYY-MM-DD)", lastDay, { lastDay = it },
                    "Masukkan tanggal hari kerja terakhir", Icons.Default.CalendarMonth,
                    onChooseDate = { dateToPick = "last" })
                CareerLetterField("Alasan (opsional)", reason, { reason = it },
                    "Tuliskan alasan pengunduran diri (opsional)", Icons.Default.EditNote, multiline = true)
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = {
                        try {
                            content = CareerLetters.resignation(
                                name, company, position, LocalDate.parse(date), LocalDate.parse(lastDay), reason
                            )
                            error = ""
                            preview = false
                        } catch (e: Exception) {
                            error = e.message ?: "Periksa isian tanggal dan format YYYY-MM-DD."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = letterBlue, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Buat Draf Resign", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
                if (error.isNotBlank()) {
                    Surface(color = screenCanvas(Color(0xFFFFF1EC)), shape = RoundedCornerShape(10.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                    }
                }
                if (content.isNotBlank()) {
                    HorizontalDivider(color = screenCanvas(letterBorder))
                    Text("Draf surat (tersimpan otomatis)", color = ScreenText,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    CareerLetterField("Isi surat", content, { content = it; preview = false },
                        "Isi surat pengunduran diri", Icons.Default.Article, multiline = true, large = true)
                    OutlinedButton(onClick = { preview = !preview },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, letterBlue),
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Description, contentDescription = null,
                            modifier = Modifier.size(19.dp), tint = letterBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(if (preview) "Tutup Preview" else "Preview A4", color = letterBlue,
                            fontWeight = FontWeight.Bold)
                    }
                    val pages = remember(content) { CvRenderer.letterPages(content) }
                    if (preview) CvDocumentPreview(pages, Modifier.height(540.dp).fillMaxWidth())
                    PdfExportButton(pages, "KarirMate-Surat-Resign.pdf")
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CareerLetterIntroCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, screenCanvas(letterBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("KARIRMATE", fontSize = 12.sp, color = letterBlue, fontWeight = FontWeight.ExtraBold)
                Surface(shape = RoundedCornerShape(50),
                    color = screenCanvas(Color(0xFFFFF1E5)),
                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFF6D9B5)))) {
                    Text("PUSAT KARIER", color = letterBlue, fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(81.dp).background(screenCanvas(Color(0xFFE2F3FF)), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Email, contentDescription = null,
                        modifier = Modifier.size(60.dp), tint = Color(0xFF2C86E6))
                    Icon(Icons.Default.Description, contentDescription = null,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp).size(35.dp),
                        tint = Color(0xFF0860B4))
                }
                Box(Modifier.width(1.dp).height(83.dp).background(screenCanvas(Color(0xFFD3E1ED))))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Surat Karier", color = ScreenText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Susun surat lamaran resmi dan dokumen pengunduran diri profesional.",
                        color = ScreenMuted, fontSize = 12.5.sp, lineHeight = 18.sp)
                }
            }
            HorizontalDivider(color = screenCanvas(Color(0xFFE0E6ED)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Kembangkan kariermu selangkah demi selangkah", color = letterBlue,
                    fontSize = 11.5.sp, modifier = Modifier.weight(1f), maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Box(Modifier.width(1.dp).height(22.dp).background(screenCanvas(Color(0xFFE0E6ED))))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Tutup" else "Info", color = letterBlue, fontWeight = FontWeight.Bold)
                }
            }
            AnimatedVisibility(expanded) {
                Text("Gunakan profil dan CV Anda untuk membuat surat lamaran atau menyusun draf resign. " +
                    "Periksa kembali informasi sebelum menyimpan PDF.",
                    color = ScreenMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun CareerLetterCreateCard(onApplicationLetter: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        border = BorderStroke(1.dp, screenCanvas(letterBorder)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color(0xFFDCEEFF).copy(alpha = .8f), radius = size.height * .75f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 1.06f, size.height * .46f))
                drawCircle(Color(0xFFECF6FF), radius = size.height * .49f,
                    center = androidx.compose.ui.geometry.Offset(size.width * .98f, size.height * .50f))
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Box(Modifier.size(75.dp).background(screenCanvas(Color(0xFFE0F1FF)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, contentDescription = null,
                            tint = letterBlue, modifier = Modifier.size(44.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Surat Lamaran Otomatis", color = ScreenText,
                            fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 21.sp)
                        Text("Gunakan profil dan CV tersimpan untuk menyusun surat lamaran otomatis.",
                            color = ScreenMuted, fontSize = 12.5.sp, lineHeight = 18.sp)
                    }
                }
                Button(onClick = onApplicationLetter,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = letterBlue, contentColor = Color.White)) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buat Surat Lamaran Sekarang", fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun CareerLetterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    multiline: Boolean = false,
    large: Boolean = false,
    onChooseDate: (() -> Unit)? = null
) {
    val calendarTrailing: (@Composable () -> Unit)? = if (onChooseDate != null) {
        {
            IconButton(onClick = onChooseDate) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pilih $label",
                    tint = screenCanvas(Color(0xFF687F9C), Color(0xFFB4CBDD)),
                    modifier = Modifier.size(20.dp))
            }
        }
    } else null
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = ScreenText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().then(if (multiline) Modifier.heightIn(min = if (large) 205.dp else 77.dp) else Modifier.heightIn(min = 49.dp)),
            singleLine = !multiline,
            minLines = if (large) 10 else if (multiline) 2 else 1,
            textStyle = TextStyle(color = ScreenText, fontSize = 12.5.sp),
            placeholder = { Text(placeholder, fontSize = 12.sp, color = ScreenMuted,
                maxLines = if (multiline) 3 else 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp),
                tint = screenCanvas(Color(0xFF7A90A8), Color(0xFFB4CBDD))) },
            trailingIcon = calendarTrailing,
            shape = RoundedCornerShape(12.dp),
            colors = karirInputColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CareerLetterDateDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    val initialMillis = remember(initial) {
        runCatching { LocalDate.parse(initial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                onDismiss()
            }) { Text("Pilih tanggal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    ) { DatePicker(state = state) }
}
