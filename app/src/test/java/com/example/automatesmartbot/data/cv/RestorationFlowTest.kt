package com.example.automatesmartbot.data.cv

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.data.product.ProductPolicy
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RestorationFlowTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    @Before fun setup() { CvRenderer.initialize(app) }
    @Test fun nineStepsSaveAndResumePerDocumentWithoutChangingTemplateOrContent() {
        val vm = CvBuilderViewModel(app, SavedStateHandle()); vm.newDocument()
        vm.updateContent { CvTemplates.sample }; val first = vm.state.value.activeId
        vm.navigate("CV Saya"); vm.resumeWizard()
        CvWizard.steps.dropLast(1).forEach { step ->
            assertEquals(step, vm.history.value.last()); assertTrue(vm.continueWizard())
        }
        assertEquals("Ekspor PDF", vm.history.value.last())
        vm.setTemplate("modern_02")
        val fresh = CvBuilderViewModel(app, SavedStateHandle()); fresh.resumeWizard()
        assertEquals(first, fresh.state.value.activeId); assertEquals("Ekspor PDF", fresh.history.value.last())
        assertEquals("modern_02", fresh.state.value.templateId); assertEquals(CvTemplates.sample, fresh.state.value.draft)
        fresh.newDocument(); fresh.resumeWizard(); assertEquals("Informasi Pribadi",fresh.history.value.last())
        fresh.selectDocument(first!!); fresh.resumeWizard(); assertEquals("Ekspor PDF", fresh.history.value.last())
    }
    @Test fun backUpdatesResumeCheckpointToTheVisibleStep() {
        val vm = CvBuilderViewModel(app, SavedStateHandle()); vm.newDocument(); vm.updateContent { CvTemplates.sample }
        vm.navigate("Informasi Pribadi"); assertTrue(vm.continueWizard()); assertTrue(vm.goBack())
        val fresh = CvBuilderViewModel(app, SavedStateHandle()); fresh.resumeWizard()
        assertEquals("Informasi Pribadi", fresh.history.value.last())
    }
    @Test fun invalidPersonalDataCannotAdvanceAndDraftIsRetained() {
        val vm = CvBuilderViewModel(app, SavedStateHandle()); vm.newDocument()
        vm.updateContent { it.copy(contact = CvContact(fullName="Dina",email="invalid")) }; vm.navigate("Informasi Pribadi")
        assertFalse(vm.continueWizard()); assertEquals("Informasi Pribadi",vm.history.value.last())
        assertTrue("email" in vm.state.value.validation)
        assertEquals("Dina", CvBuilderViewModel(app,SavedStateHandle()).state.value.draft.contact.fullName)
    }
    @Test fun productPolicyRetainsFourteenIdsAndFourAtsFreeDesigns() {
        val policy = ProductPolicy(app)
        assertTrue(policy.approved); assertEquals(14,CvTemplates.all.size)
        val free = CvTemplates.all.filter { policy.templateTier(it.id)=="free" }
        assertEquals(setOf("ats_01","ats_02","minimal_01","minimal_02"),free.map { it.id }.toSet())
        assertTrue(free.all { "ATS" in policy.templateCategories(it.id,it.category) && !it.supportsPhoto })
        assertEquals(10,CvTemplates.all.count { policy.templateTier(it.id)=="pro" })
        assertFalse(policy.canUseTemplate("modern_02",false));assertTrue(policy.canUseTemplate("modern_02",true))
    }
    @Test fun bothCvSourcesUseRealTextAndNoAnalysisForEmptyExtraction() {
        val own=PdfCvAnalyzer.analyzeContent(CvTemplates.sample)
        assertEquals(CvTemplates.sample,own.cv);assertTrue(own.strengths.any { "email" in it })
        val external=PdfCvAnalyzer.analyzeText("Dina Pratama\nEmail dina@example.com Telepon 081234567890\nProfil: Staf administrasi dengan pengalaman dokumen.\nPengalaman: Menyusun laporan mingguan.\nPendidikan: SMK Administrasi\nKeterampilan: Excel dan pengarsipan", "PDF eksternal")
        assertTrue(external.cv.summary.contains("Staf administrasi"));assertTrue(external.cv.skills.isNotEmpty())
        assertThrows(IllegalArgumentException::class.java) { PdfCvAnalyzer.analyzeText("", "scan") }
    }
}
