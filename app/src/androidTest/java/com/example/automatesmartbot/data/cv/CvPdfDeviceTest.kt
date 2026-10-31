package com.example.automatesmartbot.data.cv

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Must run on Android: Robolectric does not implement native PdfDocument/PdfRenderer. */
@RunWith(AndroidJUnit4::class)
class CvPdfDeviceTest {
    @Test fun actualExportIsA4MultiPageReadableAndIncludesPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photo = File(context.filesDir, "pdf-test-photo.jpg")
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }.let { b -> photo.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 100, it) } }
        val content = CvTemplates.sample.copy(contact = CvTemplates.sample.contact.copy(photoPath = photo.path), experience = (1..85).map { CvExperience(id = "$it", title = "Role $it", company = "Company $it", bullets = listOf("Verified achievement $it")) })
        val pages = CvRenderer.layout(content, "professional_01")
        val output = File(context.cacheDir, "native-cv-test.pdf")
        try {
            output.outputStream().use { CvRenderer.writePdf(pages, it) }
            PDFBoxResourceLoader.init(context)
            PDDocument.load(output).use { pdf ->
                assertTrue(pdf.numberOfPages > 1); assertEquals(pages.size, pdf.numberOfPages)
                assertEquals(595f, pdf.getPage(0).mediaBox.width); assertEquals(842f, pdf.getPage(0).mediaBox.height)
                assertTrue(PDFTextStripper().getText(pdf).contains("Verified achievement 85"))
            }
            PdfRenderer(ParcelFileDescriptor.open(output, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val pixel = bitmap.getPixel(112, 80)
                    assertTrue(Color.red(pixel) > 220 && Color.green(pixel) < 40)
                }
            }
        } finally { photo.delete(); output.delete() }
    }
    @Test fun editedLetterExportsAcrossPages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val text = "Surat hasil edit pengguna\n\n" + (1..100).joinToString("\n") { "Paragraf $it dengan informasi yang ditulis pengguna." }
        val output = File(context.cacheDir, "letter-test.pdf")
        try {
            output.outputStream().use { CvRenderer.writePdf(CvRenderer.letterPages(text), it) }
            PDFBoxResourceLoader.init(context)
            PDDocument.load(output).use { pdf ->
                assertTrue(pdf.numberOfPages > 1)
                assertTrue(PDFTextStripper().getText(pdf).contains("Paragraf 100"))
            }
        } finally { output.delete() }
    }
}
