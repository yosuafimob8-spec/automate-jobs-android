package com.example.automatesmartbot.data.career

import android.app.Application
import com.example.automatesmartbot.data.cv.CvRenderer
import com.example.automatesmartbot.data.product.ProductPolicy
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LearningStoreTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    @Before fun setup() { app.getSharedPreferences("career_learning_v1",0).edit().clear().commit(); CvRenderer.initialize(app) }
    @Test fun eightFieldsHaveSourcesObjectivesAndConfiguredFreeAccess() {
        val courses=LearningCatalog.load(app).courses;val policy=ProductPolicy(app)
        assertEquals(8,courses.size);assertEquals(8,courses.map { it.id }.distinct().size)
        courses.forEach { c ->
            assertEquals("free",policy.trainingTier(c.id));assertTrue(c.modules.size >= 2)
            c.modules.forEach { m ->
                assertTrue(m.objective.isNotBlank() && m.content.length>100 && m.practice.isNotEmpty())
                m.references.forEach { r -> assertTrue(r.publisher.isNotBlank() && r.title.isNotBlank());assertTrue(r.url.startsWith("https://"));assertEquals("2026-10-31",r.checkedOn) }
            }
        }
    }
    @Test fun allEightLearningPathsPersistExercisesAndExportHonestCertificates() {
        val legacy=app.getSharedPreferences("career_training",0);legacy.edit().putString("certificates","[{\"id\":\"old-user-data\"}]").commit()
        LearningCatalog.load(app).courses.forEach { c ->
            val store=LearningStore(app)
            assertThrows(IllegalArgumentException::class.java) { store.completeModule(c,1) }
            assertThrows(IllegalArgumentException::class.java) { store.submit(c,"Dina",80) }
            c.modules.forEachIndexed { n,m -> m.practice.forEachIndexed { i,q -> store.answer(c,"${m.id}_$i",q.correct) };store.completeModule(c,n) }
            store.answer(c,"simulation",c.simulation.correct);store.completeSimulation(c)
            c.exam.forEachIndexed { n,q -> store.answer(c,"exam_$n",(q.correct+1)%q.options.size) }
            assertNull(store.submit(c,"Dina",80).certificate)
            c.exam.forEachIndexed { n,q -> store.answer(c,"exam_$n",q.correct) }
            val passed=store.submit(c,"Dina",80);val certificate=passed.certificate!!
            assertEquals(100,certificate.score);assertEquals(2,LearningStore(app).load(c.id).attempts.size)
            assertEquals(c.modules.size,LearningStore(app).load(c.id).completedModules.size)
            val again=store.submit(c,"Dina",80).certificate!!;assertEquals(certificate.id,again.id)
            val bytes=ByteArrayOutputStream(); CvRenderer.writePdf(app,CvRenderer.letterPages(certificate.text()),bytes)
            PDDocument.load(bytes.toByteArray()).use { pdf -> val text=PDFTextStripper().getText(pdf);assertTrue(text.contains("Dina"));assertTrue(text.contains("bukan sertifikasi kompetensi resmi BNSP"));assertTrue(text.contains(certificate.id)) }
            assertTrue(certificate.asCvCertification().issuer.contains("bukan"))
        }
        assertEquals("[{\"id\":\"old-user-data\"}]",legacy.getString("certificates",null))
    }
    @Test fun fieldErrorsDoNotMutateUserInputAndLegalHourLimitsAreSpecific() {
        val fields=mutableMapOf("gross" to "5000000","base" to "5000000","hours" to "5","health" to "","jht" to "5000000","pension" to "0","ptkp" to "54000000","months" to "12")
        val copy=fields.toMap();val invalid=PayrollForm.validate(fields,PayrollCalculator.OvertimeDay.WORKDAY)
        assertNull(invalid.input);assertEquals(setOf("hours","health"),invalid.errors.keys);assertEquals(copy,fields)
        fields["hours"]="4";fields["health"]="5000000"
        fields["thr"]="4000000"
        val input=PayrollForm.validate(fields,PayrollCalculator.OvertimeDay.WORKDAY).input!!
        assertEquals(4_000_000L,PayrollCalculator.calculate(input).thr)
        assertEquals(PayrollCalculator.calculate(input),PayrollCalculator.calculate(input.copy()))
        assertThrows(IllegalArgumentException::class.java) { PayrollCalculator.overtime(5_000_000,12,PayrollCalculator.OvertimeDay.REST_SIX_DAY_WEEK) }
        assertThrows(IllegalArgumentException::class.java) { PayrollCalculator.overtime(5_000_000,10,PayrollCalculator.OvertimeDay.SHORT_DAY_REST) }
    }
}
