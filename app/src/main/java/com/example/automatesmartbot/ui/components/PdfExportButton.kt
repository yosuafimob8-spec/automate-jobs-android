package com.example.automatesmartbot.ui.components

import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.automatesmartbot.data.cv.CvPage
import com.example.automatesmartbot.data.cv.CvRenderer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File

/** Snapshot is private and survives picker/activity recreation. No success on failed writes. */
@Composable
fun PdfExportButton(pages: List<CvPage>, filename: String, label: String = "Ekspor PDF") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val path = snapshot
        if (uri == null) { path?.let { File(it).delete() }; snapshot = null }
        else if (path != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val type = object : TypeToken<List<CvPage>>() {}.type
                    val saved: List<CvPage> = Gson().fromJson(File(path).readText(), type)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { CvRenderer.writePdf(context, saved, it) } ?: error("Lokasi tidak dapat ditulis.")
                }
                message = "PDF berhasil disimpan."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = "Ekspor gagal: ${e.message}" }
            finally { busy = false; File(path).delete(); snapshot = null }
        }
    }
    Button(enabled = !busy && pages.isNotEmpty(), onClick = {
        try {
            val file = File.createTempFile("career-pdf-", ".json", context.cacheDir)
            file.writeText(Gson().toJson(pages)); snapshot = file.path; picker.launch(filename)
        } catch (e: Exception) { message = "PDF belum dapat disiapkan: ${e.message}" }
    }) { Text(if (busy) "Menyimpan..." else label) }
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
