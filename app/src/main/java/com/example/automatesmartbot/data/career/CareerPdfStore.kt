package com.example.automatesmartbot.data.career

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import com.example.automatesmartbot.data.cv.readBytesLimited
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import java.io.File
import java.util.UUID

data class CareerPdf(val id: String, val name: String, val path: String, val pages: Int, val createdAt: Long)
class CareerPdfStore(private val context: Context) {
    private val dir = File(context.filesDir,"career_documents").apply { mkdirs() }
    private val index = AtomicFile(File(dir,"index.json"))
    init { PDFBoxResourceLoader.init(context) }
    fun list(): List<CareerPdf> = if (!index.baseFile.isFile) emptyList() else {
        val type=object:TypeToken<List<CareerPdf>>(){}.type
        Gson().fromJson<List<CareerPdf>>(index.openRead().bufferedReader().use{it.readText()},type).orEmpty()
    }
    private fun record(file: File,name: String): CareerPdf {
        val count=PDDocument.load(file).use{pdf -> require(!pdf.isEncrypted && pdf.numberOfPages in 1..100){"PDF harus 1–100 halaman tanpa kata sandi."};pdf.numberOfPages}
        val item=CareerPdf(file.nameWithoutExtension,name,file.path,count,System.currentTimeMillis())
        val updated = listOf(item) + list()
        val stream=index.startWrite()
        try {stream.write(Gson().toJson(updated).toByteArray(Charsets.UTF_8));index.finishWrite(stream)}
        catch(e:Exception){index.failWrite(stream);throw e}
        return item
    }
    fun importPdf(uri: Uri): CareerPdf {
        val name=runCatching{context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { if(it.moveToFirst()) it.getString(0) else null }}.getOrNull() ?: "Dokumen.pdf"
        val file=File(dir,"${UUID.randomUUID()}.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use{input -> file.outputStream().use{it.write(input.readBytesLimited(15*1024*1024))}} ?: error("PDF tidak dapat dibaca.")
            return record(file,name)
        }catch(e:Exception){file.delete();throw e}
    }
    fun merge(items: List<CareerPdf>): CareerPdf {
        require(items.size in 2..10){"Pilih 2–10 PDF untuk digabung."}
        require(items.sumOf{File(it.path).length()}<=30L*1024*1024 && items.sumOf{it.pages}<=100){"Gabungan maksimal 30 MB / 100 halaman."}
        val file=File(dir,"${UUID.randomUUID()}.pdf")
        try {
            val merger=PDFMergerUtility();items.forEach{merger.addSource(File(it.path))};merger.destinationFileName=file.path
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
            return record(file,"Gabungan-${items.size}-Dokumen.pdf")
        }catch(e:Exception){file.delete();throw e}
    }
    fun transform(item: CareerPdf, pageOrder: List<Int>, rotation: Int): CareerPdf {
        require(pageOrder.isNotEmpty() && pageOrder.distinct().size==pageOrder.size && pageOrder.all{it in 1..item.pages}){"Nomor halaman harus unik dan berada dalam dokumen."}
        require(rotation in listOf(0,90,180,270))
        val file=File(dir,"${UUID.randomUUID()}.pdf")
        try {
            PDDocument.load(File(item.path)).use{source -> PDDocument().use{target ->
                pageOrder.forEach{n ->
                    val original = source.getPage(n-1)
                    val imported = target.importPage(original)
                    // importPage does not copy resources inherited from the source page tree.
                    imported.resources = original.resources
                    imported.mediaBox = original.mediaBox
                    imported.cropBox = original.cropBox
                    imported.rotation = (original.rotation + rotation) % 360
                }
                target.save(file)
            }}
            return record(file,if(rotation==0)"Halaman-${item.name}" else "Rotasi-${item.name}")
        }catch(e:Exception){file.delete();throw e}
    }
}
