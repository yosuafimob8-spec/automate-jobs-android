package com.example.automatesmartbot.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🔊 Materi Audio Interaktif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KarirTextDark)
            Text("Dengarkan narasi materi secara otomatis. Ini audio pembelajaran, bukan video.", color = KarirTextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = ready,
                    onClick = {
                        tts.speak("$title. $body", TextToSpeech.QUEUE_FLUSH, null, "training-lesson")
                        speaking = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KarirOrange,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFFFE5D0),
                        disabledContentColor = KarirTextMuted
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (speaking) "Putar Ulang" else "Dengarkan", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    enabled = ready && speaking,
                    onClick = {
                        tts.stop()
                        speaking = false
                    },
                    border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = KarirTextDark,
                        disabledContentColor = KarirTextMuted
                    )
                ) {
                    Text("Berhenti")
                }
            }
        }
    }
}
