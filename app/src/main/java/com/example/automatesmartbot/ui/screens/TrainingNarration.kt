package com.example.automatesmartbot.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Offline narrated lesson; not presented as a recorded video. */
@Composable
fun TrainingNarration(title: String, body: String) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }
    }
    DisposableEffect(tts) {
        onDispose { tts.stop(); tts.shutdown() }
    }
    LaunchedEffect(ready) {
        if (ready) tts.language = Locale("id", "ID")
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔊 Materi Audio Interaktif", style = MaterialTheme.typography.titleMedium)
            Text("Dengarkan narasi materi secara otomatis. Ini audio pembelajaran, bukan video.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = ready, onClick = {
                    tts.speak("$title. $body", TextToSpeech.QUEUE_FLUSH, null, "training-lesson")
                    speaking = true
                }) { Text(if (speaking) "Putar Ulang" else "Dengarkan") }
                OutlinedButton(enabled = ready && speaking, onClick = {
                    tts.stop()
                    speaking = false
                }) { Text("Berhenti") }
            }
        }
    }
}
