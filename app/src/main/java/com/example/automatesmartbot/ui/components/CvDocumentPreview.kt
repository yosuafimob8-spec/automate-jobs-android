package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.cv.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CvDocumentPreview(pages: List<CvPage>, modifier: Modifier = Modifier) {
    var zoom by remember { mutableFloatStateOf(1f) }
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { zoom = (zoom - .25f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Text("−") }
            Text("${(zoom * 100).toInt()}% • A4 • ${pages.size} halaman", Modifier.padding(top = 12.dp))
            TextButton(onClick = { zoom = (zoom + .25f).coerceAtMost(3f) }, enabled = zoom < 3f) { Text("+") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val pageWidth = maxWidth * zoom
            Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).pointerInput(Unit) {
                detectTransformGestures { _, _, scale, _ -> zoom = (zoom * scale).coerceIn(1f, 3f) }
            }) {
                LazyColumn(Modifier.width(pageWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(pages) { index, page -> CvPageImage(page, index + 1, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
fun CvPageImage(page: CvPage, number: Int, modifier: Modifier = Modifier, width: Int = 1000) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, page, width) {
        value = withContext(Dispatchers.Default) { CvRenderer.bitmap(page, number, width) }
    }
    // Bitmap lifetimes follow Compose image references; do not recycle an image still being drawn.
    Surface(modifier.aspectRatio(CvRenderer.WIDTH.toFloat() / CvRenderer.HEIGHT), color = androidx.compose.ui.graphics.Color.White) {
        bitmap?.let { Image(it.asImageBitmap(), "CV A4, halaman $number, ${page.template.name}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            ?: Box(Modifier.padding(16.dp)) { CircularProgressIndicator() }
    }
}
