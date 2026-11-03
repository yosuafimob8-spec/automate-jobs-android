package com.example.automatesmartbot.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.automatesmartbot.data.cv.CvSelectionStore
import com.example.automatesmartbot.ui.theme.CardBorder
import com.example.automatesmartbot.ui.theme.DarkBackground
import com.example.automatesmartbot.ui.theme.DarkSurface
import com.example.automatesmartbot.ui.theme.DarkSurfaceVariant
import com.example.automatesmartbot.ui.theme.LightBlueAccent
import com.example.automatesmartbot.ui.theme.TextGray
import com.example.automatesmartbot.ui.theme.TextWhite
import com.example.automatesmartbot.util.CvValidator

@Composable
fun CvSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember(context) { CvSelectionStore(context) }
    var selection by remember { mutableStateOf(store.getSelection()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = store.read(uri)
            val readSelection = result.selection
            if (readSelection == null) {
                errorMessage = result.validation?.message ?: "CV tidak dapat dibaca."
            } else {
                store.persistReadPermission(uri)
                store.saveSelection(readSelection)
                selection = readSelection
                errorMessage = null
            }
        }
    }

    DisposableEffect(lifecycleOwner, store) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                selection = store.getSelection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = com.example.automatesmartbot.ui.theme.KarirOrange
                    )
                    Text(
                        text = "CV Saya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = com.example.automatesmartbot.ui.theme.KarirTextDark
                    )
                }
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFDEC9))
                ) {
                    Text(
                        text = "PDF",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }

            if (selection == null) {
                Text(
                    text = "Belum ada CV. Pilih file PDF maksimal 2 MB.",
                    fontSize = 12.sp,
                    color = com.example.automatesmartbot.ui.theme.KarirTextMuted
                )
                Button(
                    onClick = { picker.launch(arrayOf("application/pdf")) },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.automatesmartbot.ui.theme.KarirOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("Pilih CV PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Surface(
                    color = Color(0xFFFFFBF7),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = selection?.displayName.orEmpty(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.automatesmartbot.ui.theme.KarirTextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${CvValidator.formatSize(selection?.sizeBytes ?: 0L)} • tersimpan di perangkat",
                            fontSize = 11.sp,
                            color = com.example.automatesmartbot.ui.theme.KarirTextMuted
                        )
                    }
                }

                Text(
                    text = "CV ini tidak dikirim otomatis. Anda tetap dapat mengganti atau menghapusnya kapan saja.",
                    fontSize = 11.sp,
                    color = com.example.automatesmartbot.ui.theme.KarirTextMuted,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, com.example.automatesmartbot.ui.theme.KarirOrange),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = com.example.automatesmartbot.ui.theme.KarirOrange
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ganti", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            store.releaseReadPermission(selection)
                            store.clearSelection()
                            selection = null
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
