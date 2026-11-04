package com.example.automatesmartbot.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.navigation.CvNavigation
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CvBuilderUiState(
    val drafts: List<CvDocument> = emptyList(), val activeId: String? = null,
    val draft: CvContent = CvContent(), val templateId: String = "ats_01",
    val message: String? = null, val ats: CvAtsReport? = null,
    val analysis: PdfCvAnalysis? = null, val analyses: List<PdfCvAnalysis> = emptyList(),
    val analysisError: String? = null, val isAnalyzing: Boolean = false,
    val coverLetter: CvCoverLetter? = null, val letters: List<CvCoverLetter> = emptyList(),
    val photoLoading: Boolean = false
)

class CvBuilderViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val store = CvDraftStore(application)
    private val letterStore = CvLetterStore(application)
    private val prefs = application.getSharedPreferences("cv_builder_active", 0)
    private val analysisDir = File(application.filesDir, "cv_analyses").apply { mkdirs() }
    private val _state = MutableStateFlow(CvBuilderUiState())
    val state = _state.asStateFlow()
    val history = savedState.getStateFlow("cv_history", listOf(CvNavigation.HOME))

    init { CvRenderer.initialize(application); loadDrafts() }
    fun navigate(page: String) { savedState["cv_history"] = CvNavigation.push(history.value, page) }
    fun goBack(): Boolean {
        if (history.value.size <= 1) return false
        savedState["cv_history"] = CvNavigation.pop(history.value)
        return true
    }
    fun resetNavigation() { savedState["cv_history"] = listOf(CvNavigation.HOME) }
    private var verifiedProAccess: Boolean = false
    fun setMembershipAccess(verified: Boolean) {
        verifiedProAccess = verified
        if (!CvTemplates.canUse(_state.value.templateId, verified)) loadDrafts()
    }
    fun loadDrafts() {
        val drafts = store.listDraftIds().mapNotNull(store::load).sortedByDescending { it.updatedAt }
        val accessible = drafts.filter { CvTemplates.canUse(it.templateId, verifiedProAccess) }
        val active = accessible.firstOrNull { it.id == prefs.getString("active", null) } ?: accessible.firstOrNull()
        val analyses = analysisDir.listFiles()?.filter { it.extension == "json" }?.mapNotNull {
            runCatching { Gson().fromJson(it.readText(), PdfCvAnalysis::class.java) }.getOrNull()
        }?.sortedByDescending { it.analyzedAt }.orEmpty()
        val letters = letterStore.list()
        val selectedLetter = letters.firstOrNull { it.id == prefs.getString("letter", null) }
        _state.value = _state.value.copy(drafts = drafts, activeId = active?.id, draft = active?.content ?: CvContent(),
            templateId = active?.templateId ?: "ats_01", letters = letters, coverLetter = selectedLetter, analyses = analyses,
            analysis = analyses.firstOrNull { it.analyzedAt == prefs.getLong("analysis", -1) })
    }
    fun newDocument() {
        val document = emptyCvDocument(templateId = "ats_01")
        try {
            store.save(document)
            prefs.edit().putString("active", document.id).apply()
            _state.value = _state.value.copy(activeId = document.id, draft = document.content, templateId = document.templateId,
                drafts = listOf(document) + _state.value.drafts, message = null, ats = null)
        } catch (e: Exception) { message("Draf gagal dibuat: ${e.message}") }
    }
    fun selectDocument(id: String, verifiedPro: Boolean = false): Boolean {
        val document = store.load(id) ?: run { message("Draf tidak dapat dibaca."); return false }
        if (!CvTemplates.canUse(document.templateId, verifiedPro && verifiedProAccess)) {
            message("Draf menggunakan template PRO. Perpanjang langganan untuk membukanya.")
            return false
        }
        prefs.edit().putString("active", id).apply()
        _state.value = _state.value.copy(activeId = id, draft = document.content, templateId = document.templateId, ats = null)
        return true
    }
    fun updateContent(transform: (CvContent) -> CvContent) {
        if (_state.value.activeId == null) newDocument()
        val current = _state.value
        if (!CvTemplates.canUse(current.templateId, verifiedProAccess)) {
            message("Akses template PRO tidak aktif."); return
        }
        _state.value = current.copy(draft = transform(current.draft), ats = null)
        persist()
    }
    fun setTemplate(id: String, verifiedPro: Boolean = false): Boolean {
        if (!CvTemplates.canUse(id, verifiedPro && verifiedProAccess)) {
            message("Template PRO memerlukan akun PRO terverifikasi.")
            return false
        }
        if (_state.value.activeId == null) newDocument()
        _state.value = _state.value.copy(templateId = id)
        return persist()
    }
    private fun persist(): Boolean {
        val current = _state.value
        if (!CvTemplates.canUse(current.templateId, verifiedProAccess)) {
            message("Akses template PRO tidak aktif."); return false
        }
        val old = current.drafts.firstOrNull { it.id == current.activeId } ?: return false
        val document = old.copy(name = current.draft.contact.fullName.ifBlank { "CV Saya" }, content = current.draft,
            templateId = current.templateId, updatedAt = System.currentTimeMillis())
        return try {
            store.save(document)
            _state.value = current.copy(drafts = current.drafts.map { if (it.id == document.id) document else it })
            true
        } catch (e: Exception) { message("Perubahan belum tersimpan: ${e.message}"); false }
    }
    fun saveDocument(): Boolean = persist().also { if (it) message("CV tersimpan di perangkat.") }
    fun deleteDocument(id: String) {
        val photo = store.load(id)?.content?.contact?.photoPath.orEmpty()
        store.delete(id)
        if (_state.value.activeId == id) prefs.edit().remove("active").apply()
        loadDrafts()
        CvPhotoStore(getApplication()).deleteIfUnused(photo, _state.value.drafts)
    }
    fun importPhoto(uri: Uri) = viewModelScope.launch {
        val targetId = _state.value.activeId ?: return@launch
        _state.value = _state.value.copy(photoLoading = true)
        try {
            val path = withContext(Dispatchers.IO) { CvPhotoStore(getApplication()).importPhoto(uri) }
            if (_state.value.activeId == targetId) updateContent { it.copy(contact = it.contact.copy(photoPath = path)) }
            else store.load(targetId)?.let { document ->
                val changed = document.copy(content = document.content.copy(contact = document.content.contact.copy(photoPath = path)), updatedAt = System.currentTimeMillis())
                store.save(changed)
                _state.value = _state.value.copy(drafts = _state.value.drafts.map { if (it.id == targetId) changed else it })
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { message("Foto gagal dipilih: ${e.message}") }
        finally { _state.value = _state.value.copy(photoLoading = false) }
    }
    fun removePhoto() {
        val old = _state.value.draft.contact.photoPath
        updateContent { it.copy(contact = it.contact.copy(photoPath = "")) }
        val saved = _state.value.activeId?.let(store::load)
        if (saved?.content?.contact?.photoPath == "") CvPhotoStore(getApplication()).deleteIfUnused(old, _state.value.drafts)
    }
    fun analyzePdf(uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(isAnalyzing = true, analysis = null, analysisError = null)
        try {
            val result = withContext(Dispatchers.IO) {
                val text = PdfCvAnalyzer.extract(getApplication(), uri)
                PdfCvAnalyzer.analyzeText(text, uri.lastPathSegment?.substringAfterLast('/') ?: "PDF")
            }
            withContext(Dispatchers.IO) {
                val file = android.util.AtomicFile(File(analysisDir, "${newCvId()}.json"))
                val output = file.startWrite()
                try { output.write(Gson().toJson(result).toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
                catch (e: Exception) { file.failWrite(output); throw e }
            }
            prefs.edit().putLong("analysis", result.analyzedAt).apply()
            _state.value = _state.value.copy(analysis = result, analyses = listOf(result) + _state.value.analyses)
            if (history.value.last() == "CV Analyst") navigate("Hasil Analisis")
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _state.value = _state.value.copy(analysis = null, analysisError = e.message ?: "Ekstraksi PDF gagal.") }
        finally { _state.value = _state.value.copy(isAnalyzing = false) }
    }
    fun selectAnalysis(analysis: PdfCvAnalysis) { prefs.edit().putLong("analysis", analysis.analyzedAt).apply(); _state.value = _state.value.copy(analysis = analysis); navigate("Hasil Analisis") }
    fun runDraftCheck() { _state.value = _state.value.copy(ats = CvAtsChecker.check(_state.value.draft, _state.value.draft.latexStyle)) }
    fun generateCoverLetter(jobTitle: String, company: String, type: LetterType, useAnalyzedCv: Boolean): Boolean {
        return try {
            val source = if (useAnalyzedCv) requireNotNull(_state.value.analysis) { "Analisis PDF belum tersedia." }.cv else _state.value.draft
            val letter = CvCoverLetterGenerator.generate(source, jobTitle, company, type)
            letterStore.save(letter)
            prefs.edit().putString("letter", letter.id).apply()
            _state.value = _state.value.copy(coverLetter = letter, letters = letterStore.list())
            true
        } catch (e: Exception) { message(e.message ?: "Surat gagal dibuat."); false }
    }
    fun selectLetter(letter: CvCoverLetter) { prefs.edit().putString("letter", letter.id).apply(); _state.value = _state.value.copy(coverLetter = letter) }
    fun updateLetter(content: String) {
        val letter = _state.value.coverLetter?.copy(content = content) ?: return
        _state.value = _state.value.copy(coverLetter = letter)
        try { letterStore.save(letter); _state.value = _state.value.copy(letters = letterStore.list()) }
        catch (e: Exception) { message("Surat belum tersimpan: ${e.message}") }
    }
    fun message(text: String) { _state.value = _state.value.copy(message = text) }
}
