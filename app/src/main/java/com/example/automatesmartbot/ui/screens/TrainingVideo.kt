package com.example.automatesmartbot.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted

@Composable
fun TrainingVideo(courseId: String, lessonIndex: Int, title: String = "", script: String = "") {
    val context = LocalContext.current
    val id = remember(courseId, lessonIndex) {
        context.resources.getIdentifier("training_${courseId}_${lessonIndex}", "raw", context.packageName)
    }
    if (id == 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KarirTextDark),
            border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color(0xFFFFF0E5),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "MATERI VISUAL",
                        color = Color(0xFFE65100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                var section by remember(courseId, lessonIndex) { mutableIntStateOf(0) }
                val segments = remember(script) {
                    script.split(Regex("(?<=\\.)\\s+")).fold(mutableListOf<String>()) { pages, sentence ->
                        if (pages.isEmpty() || pages.last().length + sentence.length > 170) pages.add(sentence)
                        else pages[pages.lastIndex] = pages.last() + " " + sentence
                        pages
                    }.ifEmpty { mutableListOf("Pelajari materi berikut.") }
                }
                Text(segments[section.coerceIn(0, segments.lastIndex)], color = Color(0xFFE2E8F0), lineHeight = 20.sp)
                Text("Slide ${section + 1} / ${segments.size}", color = Color(0xFFFFB787), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = section > 0,
                        onClick = { section-- },
                        border = BorderStroke(1.dp, if (section > 0) Color(0xFFFFB787) else Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sebelumnya")
                    }
                    Button(
                        enabled = section < segments.lastIndex,
                        onClick = { section++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KarirOrange,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF334155),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Berikutnya")
                    }
                }
            }
        }
        Text(
            "Slide pembelajaran tersedia; video MP4 khusus modul belum tersedia.",
            color = KarirTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
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
