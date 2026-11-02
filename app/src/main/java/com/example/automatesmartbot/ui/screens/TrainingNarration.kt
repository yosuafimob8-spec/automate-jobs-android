package com.example.automatesmartbot.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val ink = Color(0xFF152D43)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = ink)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔊 Materi Audio Interaktif", style = MaterialTheme.typography.titleMedium, color = ink)
            Text("Dengarkan narasi materi secara otomatis. Ini audio pembelajaran, bukan video.", color = ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = ready, onClick = {
                    tts.speak("$title. $body", TextToSpeech.QUEUE_FLUSH, null, "training-lesson")
                    speaking = true
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769D2), contentColor = Color.White, disabledContainerColor = Color(0xFFE2E8F0), disabledContentColor = ink)) { Text(if (speaking) "Putar Ulang" else "Dengarkan", color = if (ready) Color.White else ink) }
                OutlinedButton(enabled = ready && speaking, onClick = {
                    tts.stop()
                    speaking = false
                }, colors = ButtonDefaults.outlinedButtonColors(contentColor = ink, disabledContentColor = ink, disabledContainerColor = Color(0xFFF1F5F9))) { Text("Berhenti", color = ink) }
            }
        }
    }
}
