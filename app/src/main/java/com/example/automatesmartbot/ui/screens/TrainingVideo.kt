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
fun TrainingVideo(courseId: String, lessonIndex: Int) {
    val context = LocalContext.current
    val id = remember(courseId, lessonIndex) {
        context.resources.getIdentifier("training_${courseId}_${lessonIndex}", "raw", context.packageName)
    }
    if (id == 0) {
        Text("Video belum tersedia. Materi teks dan audio tetap dapat digunakan.")
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
