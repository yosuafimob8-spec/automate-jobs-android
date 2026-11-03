package com.example.automatesmartbot.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.automatesmartbot.data.career.*
import com.example.automatesmartbot.ui.components.AppInput
import kotlinx.coroutines.*
import java.io.File

@Composable
fun CareerDocumentsScreen(onLegacyDocuments: () -> Unit) {
    val context=LocalContext.current
    val store=remember{CareerPdfStore(context)}
    val scope=rememberCoroutineScope()
    var items by remember{mutableStateOf(store.list())}
    var selected by rememberSaveable{mutableStateOf(arrayListOf<String>())}
    var order by rememberSaveable{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var savePath by rememberSaveable{mutableStateOf<String?>(null)}
    fun operation(block:suspend ()->Unit){scope.launch{
        busy=true
        try{withContext(Dispatchers.IO){block()};items=withContext(Dispatchers.IO){store.list()};selected=arrayListOf();message="Dokumen baru tersimpan. Dokumen sumber tetap tersedia."}
        catch(e:CancellationException){throw e}
        catch(e:Exception){message=e.message ?: "Operasi PDF gagal."}
        finally{busy=false}
    }}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty())operation{require(uris.size<=10){"Pilih maksimal 10 file."};uris.forEach{store.importPdf(it)}}
    }
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
        val source=savePath
        if(uri!=null && source!=null)scope.launch{
            busy=true
            try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"wt")?.use{out->File(source).inputStream().use{it.copyTo(out)}} ?: error("Lokasi tidak dapat ditulis.")};message="PDF berhasil disimpan ke lokasi pilihan."}
            catch(e:CancellationException){throw e}
            catch(e:Exception){message="Penyimpanan gagal: ${e.message}"}
            finally{busy=false}
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        KarirMateSectionHero("Dokumen Karier", "Kelola dan ekspor dokumen PDF secara aman.")
        Text("Dokumen Karier",style=MaterialTheme.typography.titleLarge)
        Text("Kelola PDF secara lokal: gabung, ambil/susun halaman, putar, simpan, dan bagikan. File sumber tidak dihapus. PDF terenkripsi perlu dibuka terlebih dahulu dengan pemiliknya.")
        Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFFF2E8))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Perpustakaan PDF • ${items.size} dokumen", style = MaterialTheme.typography.titleMedium)
                Text("${selected.size} dokumen dipilih")
                Button(onClick = { importer.launch(arrayOf("application/pdf")) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFC54814))) { Text("+ Tambahkan PDF") }
                if (selected.isNotEmpty()) TextButton(onClick = { selected = arrayListOf() }) { Text("Batalkan Semua Pilihan") }
            }
        }
        TextButton(onClick=onLegacyDocuments){Text("Surat Izin & Dokumen Kerja")}
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotBlank())Text(message)
        if (items.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE5F6F2)), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Belum ada dokumen PDF", style = MaterialTheme.typography.titleMedium)
                    Text("Tambahkan PDF dari HP untuk mulai mengelola dokumen karier.")
                }
            }
        }
        items.forEach{item->Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)){
            Column(Modifier.padding(12.dp)){
                Row{Checkbox(item.id in selected,{checked->selected=ArrayList(if(checked)selected+item.id else selected.filter{it!=item.id})});Text("${item.name} • ${item.pages} halaman",Modifier.padding(top=12.dp))}
                Row{
                    TextButton(onClick={savePath=item.path;exporter.launch(item.name)},enabled=!busy){Text("Simpan ke HP")}
                    TextButton(onClick={
                        try{val uri=FileProvider.getUriForFile(context,"${context.packageName}.career-files",File(item.path));context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Bagikan PDF"))}
                        catch(e:Exception){message="PDF belum dapat dibagikan: ${e.message}"}
                    }){Text("Bagikan")}
                }
            }
        }}
        val chosen=items.filter{it.id in selected}
        HorizontalDivider()
        Text("Alat Pengolahan PDF", style = MaterialTheme.typography.titleLarge)
        Text("Pilih dua dokumen untuk menggabungkan, atau satu dokumen untuk menyusun dan memutar halaman.")
        Text("Urutan gabungan mengikuti urutan dokumen pada daftar.")
        Button(onClick={operation{store.merge(chosen)}},enabled=!busy && chosen.size>=2,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=androidx.compose.ui.graphics.Color(0xFFC54814))){Text("Gabungkan PDF Terpilih")}
        AppInput("Halaman untuk diambil / disusun (contoh 3,1,2)",order,{order=it})
        Button(onClick={operation{val source=chosen.single();val pages=order.split(',').map{it.trim().toIntOrNull() ?: error("Gunakan nomor halaman dipisahkan koma.")};store.transform(source,pages,0)}},enabled=!busy && chosen.size==1 && order.isNotBlank()){Text("Buat PDF dari Urutan Halaman")}
        Button(onClick={operation{val source=chosen.single();store.transform(source,(1..source.pages).toList(),90)}},enabled=!busy && chosen.size==1){Text("Putar 90° ke Dokumen Baru")}
    }
}
