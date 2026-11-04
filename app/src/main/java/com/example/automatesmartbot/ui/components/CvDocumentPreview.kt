package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CvDocumentPreview(pages: List<CvPage>, modifier: Modifier = Modifier) {
    var zoom by remember { mutableFloatStateOf(1f) }
    Column(modifier) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFF7F0),
            border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { zoom = (zoom - .25f).coerceAtLeast(1f) },
                    enabled = zoom > 1f,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (zoom > 1f) KarirOrangeDeep else KarirTextMuted)
                }
                Text(
                    "${(zoom * 100).toInt()}% • A4 • ${pages.size} hal",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarirTextDark
                )
                IconButton(
                    onClick = { zoom = (zoom + .25f).coerceAtMost(3f) },
                    enabled = zoom < 3f,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (zoom < 3f) KarirOrangeDeep else KarirTextMuted)
                }
            }
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
    Surface(
        modifier = modifier.aspectRatio(CvRenderer.WIDTH.toFloat() / CvRenderer.HEIGHT),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFF1E5DC)),
        shadowElevation = 2.dp,
        color = Color.White
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "CV A4, halaman $number, ${page.template.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } ?: Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KarirOrange, modifier = Modifier.size(32.dp))
        }
    }
}

/** Only the current 12-card catalog page is scheduled; layout/raster work stays off the UI thread. */
@Composable
fun CvTemplateThumbnail(content: CvContent, templateId: String, modifier: Modifier = Modifier) {
    val page by produceState<CvPage?>(null, content, templateId) {
        value = withContext(Dispatchers.Default) { CvRenderer.layout(content, templateId).first() }
    }
    page?.let { CvPageImage(it, 1, modifier, 360) } ?: Surface(
        modifier.aspectRatio(CvRenderer.WIDTH.toFloat() / CvRenderer.HEIGHT),
        color = Color.White
    ) { Box(contentAlignment = Alignment.Center) { Text("Menyiapkan preview", color = KarirTextMuted) } }
}
