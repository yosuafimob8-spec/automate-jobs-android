package com.example.automatesmartbot.data.career

import android.app.Application
import android.net.Uri
import com.example.automatesmartbot.data.cv.CvRenderer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CareerPdfStoreTest {
    private val app:Application get()=RuntimeEnvironment.getApplication()
    @Before fun setup(){File(app.filesDir,"career_documents").deleteRecursively();PDFBoxResourceLoader.init(app);CvRenderer.initialize(app)}
    private fun fixture(name:String,pages:Int):File {
        val file=File(app.cacheDir,"$name.pdf")
        val data=(1..pages).flatMap { CvRenderer.letterPages("$name PAGE $it\nActual selectable document text.") }
        file.outputStream().use{CvRenderer.writePdf(app,data,it)}
        return file
    }
    @Test fun repeatedImportsPreserveIndexAndSourceDocuments() {
        val store=CareerPdfStore(app);val first=store.importPdf(Uri.fromFile(fixture("ONE",2)));val second=store.importPdf(Uri.fromFile(fixture("TWO",3)))
        assertEquals(2,CareerPdfStore(app).list().size)
        assertTrue(File(first.path).isFile);assertTrue(File(second.path).isFile)
        val merged=store.merge(listOf(first,second))
        assertEquals(5,merged.pages);assertEquals(3,store.list().size)
        PDDocument.load(File(merged.path)).use{assertTrue(PDFTextStripper().getText(it).contains("TWO PAGE 3"))}
        assertTrue(File(first.path).isFile)
    }
    @Test fun extractReorderAndRotationProduceValidIndependentFiles() {
        val store=CareerPdfStore(app);val source=store.importPdf(Uri.fromFile(fixture("SOURCE",3)))
        val changed=store.transform(source,listOf(3,1),90)
        PDDocument.load(File(changed.path)).use{pdf->
            assertEquals(2,pdf.numberOfPages);assertEquals(90,pdf.getPage(0).rotation)
            val text=PDFTextStripper().apply{startPage=1;endPage=1}.getText(pdf)
            assertTrue(text, text.replace(Regex("\\s+"), "").contains("SOURCEPAGE3"))
            PDDocument.load(File(source.path)).use { original ->
                assertArrayEquals(original.getPage(2).contents.use { it.readBytes() }, pdf.getPage(0).contents.use { it.readBytes() })
            }
        }
        PDDocument.load(File(source.path)).use{assertEquals(3,it.numberOfPages);assertEquals(0,it.getPage(0).rotation)}
        assertTrue(runCatching{store.transform(source,listOf(1,1),0)}.isFailure)
        assertTrue(runCatching{store.transform(source,listOf(4),0)}.isFailure)
    }
    @Test fun invalidPdfDoesNotCreateFalseSavedEntry() {
        val file=File(app.cacheDir,"bad.pdf").apply{writeText("not a PDF")};val store=CareerPdfStore(app)
        assertTrue(runCatching{store.importPdf(Uri.fromFile(file))}.isFailure)
        assertTrue(store.list().isEmpty())
    }
    @Test fun certificatesAndResignationActuallyExportAsSelectablePdf() {
        val content="SERTIFIKAT PENYELESAIAN LATIHAN\nDina Pratama\nBukan sertifikasi BNSP\nID latihan uji"
        val file=File(app.cacheDir,"certificate.pdf")
        file.outputStream().use{CvRenderer.writePdf(app,CvRenderer.letterPages(content),it)}
        PDDocument.load(file).use{pdf->assertTrue(PDFTextStripper().getText(pdf).contains("Dina Pratama"));assertEquals(595f,pdf.getPage(0).mediaBox.width)}
    }
}
