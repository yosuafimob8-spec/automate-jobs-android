package com.example.automatesmartbot.data.cv

import android.app.Application
import android.graphics.*
import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.data.model.UserMeResponse
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CvCatalogIntegrationTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private fun init() { CvRenderer.initialize(app) }
    @Test fun all75HaveDistinctGeometryBeyondColorAndGenerateReviewSheet() {
        init()
        val signatures = mutableSetOf<String>()
        val sheet = Bitmap.createBitmap(1200, 10 * 235, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet); canvas.drawColor(Color.WHITE)
        CvTemplates.catalog.forEachIndexed { index, t ->
            val pages = CvRenderer.layout(CvTemplates.sample, t.id)
            val signature = pages.joinToString("|") { page -> page.texts.joinToString { "${it.text}:${it.x}:${it.baseline}:${it.size}:${it.bold}" } }
            assertTrue("Repeated geometry: ${t.id}", signatures.add(signature))
            val image = CvRenderer.bitmap(pages.first(), 1, 140)
            val x = (index % 8) * 150f; val y = (index / 8) * 235f
            canvas.drawBitmap(image, x, y, null); image.recycle()
            canvas.drawText(t.id, x, y + 218, Paint().apply { color = Color.BLACK; textSize = 10f })
        }
        assertEquals(75, signatures.size)
        File("build/reports/cv-catalog/contact-sheet.png").apply { parentFile!!.mkdirs(); outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        sheet.recycle()
    }
    @Test fun everyTemplatePaginatesLongUserDataWithoutClippingOrTextCollisions() {
        init()
        val content = CvTemplates.sample.copy(experience = (1..35).map { CvExperience(title = "Peran $it", company = "Organisasi $it", bullets = listOf("Laporan terverifikasi dengan tanggung jawab dan pencapaian yang dapat ditelusuri.")) }, projects = listOf(CvProject(name = "Proyek Terakhir", bullets = listOf("Penanda akhir proyek"))))
        CvTemplates.catalog.forEach { template ->
            val pages = CvRenderer.layout(content, template.id)
            assertTrue("Must paginate ${template.id}", pages.size > 1)
            val text = pages.flatMap { it.texts }.joinToString(" ") { it.text }
            assertTrue(text.contains("Peran 35")); assertTrue(text.contains("Penanda akhir proyek"))
            pages.forEach { page ->
                val bounds = page.texts.map { t ->
                    val paint = Paint().apply { textSize = t.size; typeface = CvRenderer.typeface(t.bold) }
                    assertTrue("Bounds ${template.id} ${t.text}", t.x >= 24 && t.x + paint.measureText(t.text) <= 572 && t.baseline <= 795)
                    RectF(t.x, t.baseline + paint.fontMetrics.ascent, t.x + paint.measureText(t.text), t.baseline + paint.fontMetrics.descent)
                }
                bounds.forEachIndexed { index, a -> bounds.drop(index + 1).forEach { b ->
                    val intersection = RectF(a)
                    assertFalse("Text collision ${template.id}: $a / $b", intersection.intersect(b) && intersection.width() > 1f && intersection.height() > 1f)
                } }
            }
        }
    }
    @Test fun docxContainsExactLaidOutUserTextPageBreaksAndPageArtwork() {
        init()
        val content = CvTemplates.sample.copy(summary = "Data pengguna & <dokumen>\nBaris berikutnya", experience = (1..40).map { CvExperience(title = "Peran $it", bullets = listOf("Isi asli pengguna")) })
        val pages = CvRenderer.layout(content, "modern_12")
        val bytes = ByteArrayOutputStream(); CvExporter.writeDocx(pages, bytes)
        assertTrue(bytes.size() > 1000)
        val entries = mutableMapOf<String,ByteArray>()
        ZipInputStream(bytes.toByteArray().inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { entries[entry.name] = zip.readBytes(); entry = zip.nextEntry }
        }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(entries.getValue("word/document.xml").inputStream())
        val texts = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "t")
        assertEquals(pages.flatMap { it.texts }.map { it.text }, (0 until texts.length).map { texts.item(it).textContent })
        assertEquals(pages.size, entries.keys.count { it.startsWith("word/media/") })
        assertEquals(pages.size - 1, document.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "br").length)
        entries.filterKeys { it.endsWith(".xml") || it.endsWith(".rels") }.forEach { (_, xml) -> factory.newDocumentBuilder().parse(xml.inputStream()) }
        File("build/reports/cv-catalog/sample.docx").apply { parentFile!!.mkdirs(); writeBytes(bytes.toByteArray()) }
    }
    @Test fun supportedTemplatesIncludePhotoAndUnsupportedTemplatesOmitIt() {
        init()
        val file = File(app.filesDir, "catalog-photo.png")
        Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }.let { image ->
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
        }
        val content = CvTemplates.sample.copy(contact = CvTemplates.sample.contact.copy(photoPath = file.path))
        CvTemplates.catalog.forEach { template ->
            val image = CvRenderer.bitmap(CvRenderer.layout(content, template.id).first(), 1, 300)
            val pixels = IntArray(image.width * image.height)
            image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
            val red = pixels.count { Color.red(it) > 220 && Color.green(it) < 30 && Color.blue(it) < 30 }
            if (template.supportsPhoto) assertTrue(template.id, red > 200) else assertEquals(template.id, 0, red)
            image.recycle()
        }
    }

    @Test fun actualPdfExportCoversEveryCatalogFamilyWithSelectableUserText() = runTest {
        init()
        val authority = object : CvAccessAuthority {
            override fun canUse(templateId: String) = CvTemplates.canUse(templateId, true)
            override suspend fun refresh() {}
        }
        val service = CvExportService(authority)
        CvTemplates.catalog.filter { it.design != null }.distinctBy { it.category }.forEach { template ->
            val bytes = ByteArrayOutputStream()
            service.writePdf(app, CvExportRequest(template.id, CvTemplates.sample), bytes)
            assertTrue(bytes.size() > 1000)
            com.tom_roush.pdfbox.pdmodel.PDDocument.load(bytes.toByteArray()).use { pdf ->
                val text = com.tom_roush.pdfbox.text.PDFTextStripper().getText(pdf)
                assertTrue(text.contains("Dina Pratama")); assertTrue(text.contains("Digitalisasi Arsip"))
                assertEquals(595f, pdf.getPage(0).mediaBox.width)
            }
        }
    }

    @Test fun membershipCannotBeGrantedByUiFlagsAndExpiresAtControlledTime() = runTest {
        var now = 1000L; var token: String? = "account-a"
        var response = UserMeResponse("user-a", null, true, null, true, "pro", "5000")
        val access = ServerCvAccess({ token }, { now }, { response })
        val pro = CvTemplates.catalog.first { CvTemplates.isProTemplate(it.id) }.id
        assertFalse(access.canUse(pro)); access.refresh(); assertTrue(access.canUse(pro))
        now = 5000000L; assertFalse(access.canUse(pro))
        now = 1000; access.refresh(); token = "account-b"; assertFalse(access.canUse(pro))
        token = "account-a"; response = response.copy(isPro = false, membership = "free")
        access.refresh(); assertFalse(access.canUse(pro))
        val vm = CvBuilderViewModel(app, SavedStateHandle(), access)
        vm.newDocument(); assertFalse(vm.setTemplate(pro, true))
        assertTrue(access.canUse("standard"))
    }
    @Test fun expiredProDraftSurvivesAndReturnsAfterVerifiedRenewal() = runTest {
        var active = false
        val authority = object : CvAccessAuthority {
            override fun canUse(templateId: String) = CvTemplates.canUse(templateId, active)
            override suspend fun refresh() {}
        }
        val vm = CvBuilderViewModel(app, SavedStateHandle(), authority)
        vm.newDocument(); vm.updateContent { CvTemplates.sample }
        val pro = CvTemplates.catalog.first { CvTemplates.isProTemplate(it.id) }.id
        active = true; vm.refreshMembership(); assertTrue(vm.setTemplate(pro))
        val document = vm.state.value.activeId!!
        active = false; vm.refreshMembership()
        assertTrue(vm.state.value.drafts.any { it.id == document && it.templateId == pro })
        assertFalse(vm.selectDocument(document, true))
        active = true; vm.refreshMembership(); assertTrue(vm.selectDocument(document))
        assertEquals(CvTemplates.sample, vm.state.value.draft)
    }
    @Test fun exportRechecksCapturedTemplateAndAccountBeforeWritingAnything() = runTest {
        var active = true; var identity: String? = "owner-a"
        val authority = object : CvAccessAuthority {
            override fun canUse(templateId: String) = CvTemplates.canUse(templateId, active)
            override suspend fun refresh() { active = false }
        }
        val service = CvExportService(authority) { identity }
        val pro = CvTemplates.catalog.first { CvTemplates.isProTemplate(it.id) }.id
        val bytes = ByteArrayOutputStream()
        assertTrue(runCatching { service.writePdf(app, CvExportRequest(pro, CvTemplates.sample, "owner-a"), bytes) }.isFailure)
        assertEquals(0, bytes.size())
        identity = "owner-b"
        assertTrue(runCatching { service.writeDocx(app, CvExportRequest("ats_01", CvTemplates.sample, "owner-a"), bytes) }.isFailure)
        assertEquals(0, bytes.size())
    }
    @Test fun failedAtomicSaveRetainsOldDocumentAndNeverReportsSuccess() {
        val vm = CvBuilderViewModel(app, SavedStateHandle())
        vm.newDocument(); vm.updateContent { CvTemplates.sample }
        val id = vm.state.value.activeId!!
        val destination = File(app.filesDir, "cv_drafts/$id.json")
        val original = destination.readBytes()
        val blocked = File(destination.path + ".new").apply { mkdir() }
        try {
            assertFalse(vm.saveDocument())
            assertTrue(vm.state.value.message!!.contains("belum tersimpan"))
            assertArrayEquals(original, destination.readBytes())
        } finally { blocked.delete() }
    }

    @Test fun expiryDuringRenderingNeverTruncatesExistingDestination() = runTest {
        init()
        val pro = CvTemplates.catalog.first { CvTemplates.isProTemplate(it.id) }.id
        CvExportFormat.entries.forEach { format ->
            var refreshes = 0
            val authority = object : CvAccessAuthority {
                override fun canUse(templateId: String) = refreshes == 1
                override suspend fun refresh() { refreshes++ }
            }
            val file = File(app.cacheDir, "existing-$format").apply { writeText("Dokumen lama tetap aman") }
            assertTrue(runCatching { CvExportService(authority).export(app, android.net.Uri.fromFile(file), CvExportRequest(pro, CvTemplates.sample), format) }.isFailure)
            assertEquals(2, refreshes)
            assertEquals("Dokumen lama tetap aman", file.readText())
        }
    }

    @Test fun opaqueTokenRenewalUsesVerifiedOwnerWithoutLosingDraftsOrCrossingAccounts() = runTest {
        var token: String? = "opaque-first"
        var owner = "id:owner-a"
        val authority = object : CvAccessAuthority {
            override fun canUse(templateId: String) = CvTemplates.canUse(templateId, false)
            override suspend fun refresh() {}
            override fun workspaceOwner() = owner
        }
        val first = CvBuilderViewModel(app, SavedStateHandle(), authority) { token }
        assertFalse(first.state.value.workspaceReady)
        first.newDocument(); assertNull(first.state.value.activeId)
        first.refreshMembership(); first.newDocument(); first.updateContent { CvTemplates.sample }
        val id = first.state.value.activeId
        token = "opaque-renewed"
        val renewed = CvBuilderViewModel(app, SavedStateHandle(), authority) { token }
        renewed.refreshMembership()
        assertEquals(id, renewed.state.value.activeId)
        assertEquals(CvTemplates.sample, renewed.state.value.draft)
        token = "other-account"; owner = "id:owner-b"
        val other = CvBuilderViewModel(app, SavedStateHandle(), authority) { token }
        other.refreshMembership()
        assertTrue(other.state.value.drafts.isEmpty())
    }

    @Test fun accountsAndLegacyDeviceFilesRemainSeparatedWithoutDeletion() {
        val legacy = CvDraftStore(app); val old = emptyCvDocument(templateId = "standard"); legacy.save(old)
        val a = CvDraftStore(cvWorkspace(app, "opaque-account-a"))
        val b = CvDraftStore(cvWorkspace(app, "opaque-account-b"))
        val document = emptyCvDocument(); a.save(document)
        assertNotNull(a.load(document.id)); assertNull(b.load(document.id))
        assertNull(a.load(old.id)); assertNotNull(legacy.load(old.id))
        assertNotNull(CvDraftStore(cvWorkspace(app, "opaque-account-a")).load(document.id))
    }
}
