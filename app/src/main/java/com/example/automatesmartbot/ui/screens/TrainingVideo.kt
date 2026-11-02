package com.example.automatesmartbot.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

@Composable
fun TrainingVideo(courseId: String, lessonIndex: Int, title: String = "", script: String = "") {
    val context = LocalContext.current
    val id = remember(courseId, lessonIndex) {
        context.resources.getIdentifier("training_${courseId}_${lessonIndex}", "raw", context.packageName)
    }
    if (id == 0) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF152D43))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MATERI VISUAL", color = androidx.compose.ui.graphics.Color(0xFFFFB86B))
                Text(title, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
                var section by remember(courseId, lessonIndex) { mutableIntStateOf(0) }
                val segments = remember(script) {
                    script.chunked(220).ifEmpty { listOf("Pelajari materi berikut.") }
                }
                Text(segments[section.coerceIn(0, segments.lastIndex)], color = androidx.compose.ui.graphics.Color.White)
                Text("Slide ${section + 1} / ${segments.size}", color = androidx.compose.ui.graphics.Color.LightGray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = section > 0, onClick = { section-- }) { Text("Sebelumnya") }
                    OutlinedButton(enabled = section < segments.lastIndex, onClick = { section++ }) { Text("Berikutnya") }
                }
            }
        }
        Text("Slide pembelajaran tersedia; video MP4 khusus modul belum tersedia.")
        return
    }
    AndroidView(factory = { ctx ->
        VideoView(ctx).apply {
            val controls = MediaController(ctx)
            controls.setAnchorView(this)
            setMediaController(controls)
            setVideoURI(Uri.parse("android.resource://${ctx.packageName}/$id"))
        }
    }, modifier = Modifier.fillMaxWidth().height(220.dp))
}
