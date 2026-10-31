package com.example.automatesmartbot.data.cv

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.ui.navigation.CvNavigation
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
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
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CvFlowTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    @Before fun clean() {
        CvRenderer.initialize(app)
        listOf("cv_drafts", "cv_letters", "cv_photos", "cv_analyses").forEach { File(app.filesDir, it).deleteRecursively() }
        app.getSharedPreferences("cv_builder_active", 0).edit().clear().commit()
    }
    @Test fun headerAndSystemBackPopTheSameHistory() {
        val vm = CvBuilderViewModel(app, SavedStateHandle())
        vm.navigate("CV Saya"); vm.navigate("Pendidikan")
        assertEquals(listOf("CV Builder", "CV Saya", "Pendidikan"), vm.history.value)
        assertTrue(vm.goBack()); assertEquals("CV Saya", vm.history.value.last())
        assertTrue(vm.goBack()); assertEquals(CvNavigation.HOME, vm.history.value.last())
        assertFalse(vm.goBack())
        vm.navigate("CV Analyst"); vm.navigate("Pilih Jenis Surat"); vm.navigate("Editor Surat"); vm.navigate("Preview Surat")
        assertTrue(vm.goBack()); assertEquals("Editor Surat", vm.history.value.last())
        vm.resetNavigation(); assertEquals(listOf(CvNavigation.HOME), vm.history.value)
    }
    @Test fun firstKeystrokeAndAllSectionsSurviveViewModelRecreation() {
        val vm = CvBuilderViewModel(app, SavedStateHandle())
        vm.newDocument(); vm.updateContent { CvTemplates.sample.copy(extras = listOf(CvExtraSection("languages", "Bahasa", "Indonesia"))) }
        vm.setTemplate("modern_02"); vm.navigate("Pendidikan"); vm.goBack()
        val fresh = CvBuilderViewModel(app, SavedStateHandle())
        assertEquals(vm.state.value.draft, fresh.state.value.draft)
        assertEquals("modern_02", fresh.state.value.templateId)
        assertEquals(1, fresh.state.value.drafts.size)
    }
    @Test fun oldDraftsLoadWithoutPhotoOrExtraFields() {
        val store = CvDraftStore(app)
        val document = emptyCvDocument().copy(content = CvTemplates.sample)
        store.save(document)
        val file = File(app.filesDir, "cv_drafts/${document.id}.json")
        val json = org.json.JSONObject(file.readText())
        json.getJSONObject("content").remove("extras")
        json.getJSONObject("content").getJSONObject("contact").remove("photoPath")
        file.writeText(json.toString())
        assertEquals("", store.load(document.id)!!.content.contact.photoPath)
        assertTrue(store.load(document.id)!!.content.extras.isEmpty())
    }
    @Test fun galleryPhotoIsCopiedPersistsAndCanBeRemoved() {
        val source = File(app.cacheDir, "gallery.jpg")
        val image = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        source.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val path = CvPhotoStore(app).importPhoto(Uri.fromFile(source))
        source.delete()
        assertTrue(File(path).isFile)
        val vm = CvBuilderViewModel(app, SavedStateHandle())
        vm.updateContent { it.copy(contact = it.contact.copy(fullName = "Dina", photoPath = path)) }
        assertEquals(path, CvBuilderViewModel(app, SavedStateHandle()).state.value.draft.contact.photoPath)
        vm.removePhoto()
        assertFalse(File(path).exists())
        assertEquals("", CvBuilderViewModel(app, SavedStateHandle()).state.value.draft.contact.photoPath)
    }
    @Test fun invalidImageAndOversizedStreamsAreRejected() {
        val invalid = File(app.cacheDir, "invalid.jpg").apply { writeText("not an image") }
        assertTrue(runCatching { CvPhotoStore(app).importPhoto(Uri.fromFile(invalid)) }.isFailure)
        assertTrue(runCatching { ByteArray(20).inputStream().readBytesLimited(10) }.isFailure)
    }
    @Test fun allTemplatesHaveDistinctLayoutsReadableA4ThumbnailsAndFullContent() {
        assertEquals(7, CvTemplates.all.map { it.category }.distinct().size)
        assertEquals(14, CvTemplates.all.map { it.layout }.distinct().size)
        val fingerprints = CvTemplates.all.map { t ->
            val pages = CvRenderer.layout(CvTemplates.sample, t.id)
            val text = pages.flatMap { it.texts }.joinToString(" ") { it.text }
            assertTrue(text.contains("Dina Pratama")); assertTrue(text.contains("PENDIDIKAN")); assertTrue(text.contains("PENGALAMAN"))
            assertTrue(pages.flatMap { it.texts }.all { it.color != Color.WHITE && it.baseline <= 794 && it.x >= 24 })
            val bitmap = CvRenderer.bitmap(pages.first(), 1, 300)
            assertEquals(300 * 842 / 595, bitmap.height)
            val pixels = IntArray(bitmap.width * bitmap.height); bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue(pixels.any { it != Color.WHITE })
            val previewDir = File("build/reports/cv-previews").apply { mkdirs() }
            File(previewDir, "${t.id}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            pixels.contentHashCode()
        }
        assertEquals(14, fingerprints.distinct().size)
    }
    @Test fun longCvPaginatesWithoutClippingOrLosingLastExperienceAndProject() {
        val content = CvTemplates.sample.copy(experience = (1..85).map { i -> CvExperience(id = "$i", title = "Poste $i", company = "Entreprise $i", bullets = listOf("Achievement unique $i with documented responsibilities and measurable results.")) }, projects = listOf(CvProject(name = "FinalProject", link = "https://example.com/portfolio", bullets = listOf("FinalProjectDescription"))))
        val pages = CvRenderer.layout(content, "modern_01")
        assertTrue(pages.size > 1)
        val text = pages.flatMap { it.texts }.joinToString(" ") { it.text }
        assertTrue(text.contains("Poste 85")); assertTrue(text.contains("FinalProjectDescription"))
        assertTrue(pages.flatMap { it.texts }.all { it.baseline < 795f })
    }
    @Test fun actualPdfExportIsA4SelectableMultipageAndPreservesIndonesianText() {
        val content = CvTemplates.sample.copy(experience = (1..70).map { CvExperience(title = "Peran $it", company = "Perusahaan $it", bullets = listOf("Pencapaian nyata $it — laporan dan arsip")) })
        val pages = CvRenderer.layout(content, "professional_01")
        val output = java.io.ByteArrayOutputStream()
        CvRenderer.writePdf(app, pages, output)
        PDDocument.load(output.toByteArray()).use { pdf ->
            assertEquals(pages.size, pdf.numberOfPages)
            assertTrue(pdf.numberOfPages > 1)
            assertEquals(595f, pdf.getPage(0).mediaBox.width)
            assertEquals(842f, pdf.getPage(0).mediaBox.height)
            val text = PDFTextStripper().getText(pdf)
            assertTrue(text.contains("Pencapaian nyata 70"))
            assertTrue(text.contains("Dina Pratama"))
        }
        File("build/reports/cv-previews/verified-export.pdf").apply { parentFile!!.mkdirs(); writeBytes(output.toByteArray()) }
    }
    @Test fun photoAppearsOnlyOnSupportedTemplates() {
        val file = File(app.filesDir, "photo.jpg")
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }.let { b -> file.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 100, it) } }
        val c = CvTemplates.sample.copy(contact = CvTemplates.sample.contact.copy(photoPath = file.path))
        fun redPixels(template: String): Int {
            val b = CvRenderer.bitmap(CvRenderer.layout(c, template).first(), 1, 595)
            val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
            return px.count { Color.red(it) > 220 && Color.green(it) < 30 && Color.blue(it) < 30 }
        }
        assertTrue(redPixels("professional_01") > 1000)
        assertEquals(0, redPixels("ats_01"))
        val output = ByteArrayOutputStream()
        CvRenderer.writePdf(app, CvRenderer.layout(c, "professional_01"), output)
        File("build/reports/cv-previews/verified-photo.pdf").apply { parentFile!!.mkdirs(); writeBytes(output.toByteArray()) }
        PDDocument.load(output.toByteArray()).use { pdf ->
            val images = pdf.getPage(0).resources.xObjectNames.map { pdf.getPage(0).resources.getXObject(it) }
                .filterIsInstance<com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject>()
            // PDFBox Android's getImage decoder swaps R/B on this runtime. Verify the
            // stored DeviceRGB stream itself; an independent PyMuPDF check also renders red.
            assertTrue(images.any { image ->
                if (image.colorSpace.numberOfComponents != 3 || image.bitsPerComponent != 8) false
                else {
                    val raw = image.cosObject.createInputStream().use { it.readBytes() }
                    val offset = (80 * image.width + 112) * 3
                    (raw[offset].toInt() and 255) > 220 && (raw[offset + 1].toInt() and 255) < 30
                }
            })
        }
    }
    @Test fun coverLetterTypesUseRealFactsAndPersistEdits() {
        LetterType.entries.filter { it != LetterType.AI }.forEach { type ->
            val letter = CvCoverLetterGenerator.generate(CvTemplates.sample, "Admin", "PT Tujuan", type)
            assertTrue(letter.content.contains("Dengan hormat")); assertTrue(letter.content.contains("Dina Pratama"))
            assertFalse(letter.content.contains("Dear Hiring")); assertTrue(letter.content.contains("PT Tujuan"))
            val store = CvLetterStore(app); store.save(letter.copy(content = "Surat hasil edit pengguna"))
            assertEquals("Surat hasil edit pengguna", store.list().first { it.id == letter.id }.content)
        }
        assertTrue(runCatching { CvCoverLetterGenerator.generate(CvTemplates.sample, "Admin", "PT Tujuan", LetterType.AI) }.isFailure)
        assertTrue(runCatching { CvCoverLetterGenerator.generate(CvTemplates.sample, "", "") }.isFailure)
        val empty = CvCoverLetterGenerator.generate(CvContent(), "Admin", "PT Tujuan")
        assertFalse(empty.content.contains("berpengalaman")); assertFalse(empty.content.contains("Microsoft"))
    }
    @Test fun analyzerRejectsMissingTextAndAnalyzesExtractedPdfWithoutJobDescription() {
        assertTrue(runCatching { PdfCvAnalyzer.analyzeText("", "scan.pdf") }.isFailure)
        val output = File(app.cacheDir, "cv.pdf")
        // PDFBox creates an actual selectable-text PDF fixture. Native PdfDocument export
        // is separately covered by CvPdfDeviceTest because Robolectric has no PDF shadow.
        PDFBoxResourceLoader.init(app)
        PDDocument().use { pdf ->
            val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
            pdf.addPage(page)
            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(pdf, page).use { stream ->
                stream.beginText(); stream.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11f)
                stream.newLineAtOffset(48f, 780f)
                listOf("Dina Pratama", "dina@example.com", "081200000000", "PROFIL", "Staf administrasi dengan pengalaman pengelolaan dokumen.", "PENGALAMAN KERJA", "Mengelola arsip dan laporan administrasi.", "PENDIDIKAN", "Universitas Contoh, Manajemen", "KETERAMPILAN", "Microsoft Excel").forEach { stream.showText(it); stream.newLineAtOffset(0f, -18f) }
                stream.endText()
            }
            pdf.save(output)
        }
        val text = PdfCvAnalyzer.extract(app, Uri.fromFile(output))
        val report = PdfCvAnalyzer.analyzeText(text, "cv.pdf")
        assertTrue(report.score in 0..100); assertTrue(report.strengths.any { "pendidikan" in it })
        assertEquals("dina@example.com", report.cv.contact.email)
        val letter = CvCoverLetterGenerator.generate(report.cv, "Admin", "PT Tujuan", LetterType.EXPERIENCE)
        assertTrue(letter.content.contains("Mengelola arsip"))
    }
}
