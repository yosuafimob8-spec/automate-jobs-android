package com.example.automatesmartbot.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.cv.CvBuilderConstants
import com.example.automatesmartbot.data.cv.CvContent
import com.example.automatesmartbot.data.cv.CvCoverLetter
import com.example.automatesmartbot.data.cv.CvCoverLetterGenerator
import com.example.automatesmartbot.data.cv.CvDraftStore
import com.example.automatesmartbot.data.cv.CvDocument
import com.example.automatesmartbot.data.cv.CvKeywordReport
import com.example.automatesmartbot.data.cv.CvStyle
import com.example.automatesmartbot.data.cv.CvAtsReport
import com.example.automatesmartbot.data.cv.CvAtsChecker
import com.example.automatesmartbot.data.cv.emptyCvDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CvBuilderUiState(
    val drafts: List<CvDocument> = emptyList(),
    val activeId: String? = null,
    val draft: CvContent = CvContent(),
    val templateId: String = CvBuilderConstants.TEMPLATE_STANDARD,
    val latexStyle: String = "classic",
    val style: CvStyle = CvStyle(),
    val saveState: String? = null,
    val message: String? = null,
    val ats: CvAtsReport? = null,
    val keyword: CvKeywordReport? = null,
    val keywordJobDescription: String? = null,
    val coverLetter: CvCoverLetter? = null,
    val isAnalyzing: Boolean = false
)

class CvBuilderViewModel(application: Application) : AndroidViewModel(application) {
    private val draftStore = CvDraftStore(application)

    private val _state = MutableStateFlow<CvBuilderUiState>(CvBuilderUiState())
    val state: StateFlow<CvBuilderUiState> = _state.asStateFlow()

    init {
        loadDrafts()
    }

    fun loadDrafts() {
        val drafts = draftStore.listDraftIds().mapNotNull { draftStore.load(it) }
        val active = drafts.firstOrNull { it.id == _state.value.activeId }
            ?: drafts.firstOrNull { isCvFilled(it.content) }
        _state.value = _state.value.copy(
            drafts = drafts,
            activeId = active?.id,
            draft = active?.content ?: CvContent(),
            templateId = active?.templateId ?: CvBuilderConstants.TEMPLATE_STANDARD,
            latexStyle = active?.content?.latexStyle ?: "classic",
            style = active?.content?.style ?: CvStyle(),
            saveState = if (active == null) null else "saved"
        )
    }

    private fun isCvFilled(c: CvContent): Boolean =
        c.contact.fullName.isNotBlank() || c.contact.email.isNotBlank() ||
        c.contact.phone.isNotBlank() || c.contact.headline.isNotBlank() ||
        c.summary.isNotBlank() || c.experience.isNotEmpty() ||
        c.education.isNotEmpty() || c.skills.isNotEmpty() ||
        c.projects.isNotEmpty() || c.certifications.isNotEmpty()


    fun newDocument() {
        // Draf baru hanya di memori; belum masuk daftar CV tersimpan.
        _state.value = _state.value.copy(
            activeId = null,
            draft = CvContent(),
            templateId = CvBuilderConstants.HTML_TEMPLATE_IDS.firstOrNull() ?: CvBuilderConstants.TEMPLATE_STANDARD,
            latexStyle = "classic",
            style = CvStyle(),
            saveState = null,
            message = null,
            ats = null,
            keyword = null,
            coverLetter = null
        )
    }


    fun selectDocument(id: String) {
        val draft = draftStore.load(id) ?: return
        _state.value = _state.value.copy(
            activeId = id,
            draft = draft.content,
            templateId = draft.templateId,
            latexStyle = draft.content.latexStyle,
            style = draft.content.style,
            saveState = "saved"
        )
    }

    fun updateContent(transform: (CvContent) -> CvContent) {
        val current = _state.value
        val updated = transform(current.draft)
        val document = current.drafts.firstOrNull { it.id == current.activeId }
        val next = document?.copy(content = updated, templateId = current.templateId, updatedAt = System.currentTimeMillis())
        if (next != null) draftStore.save(next)
        _state.value = current.copy(
            drafts = if (next == null) current.drafts else current.drafts.map { if (it.id == next.id) next else it },
            draft = updated,
            style = updated.style,
            latexStyle = updated.latexStyle,
            saveState = if (next == null) null else "saved"
        )
    }


    fun setTemplate(id: String) {
        if (id !in CvBuilderConstants.HTML_TEMPLATE_IDS) return
        val current = _state.value
        val document = current.drafts.firstOrNull { it.id == current.activeId }
        val next = document?.copy(templateId = id, updatedAt = System.currentTimeMillis())
        if (next != null) draftStore.save(next)
        _state.value = current.copy(
            templateId = id,
            saveState = if (next == null) null else "saved",
            drafts = if (next == null) current.drafts else current.drafts.map { if (it.id == next.id) next else it }
        )
    }


    fun setLatexStyle(id: String) {
        updateContent { it.copy(latexStyle = id) }
    }

    fun saveDocument(): Boolean {
        val current = _state.value
        if (!isCvFilled(current.draft)) {
            _state.value = current.copy(saveState = "error", message = "Isi data CV terlebih dahulu sebelum menyimpan.")
            return false
        }
        val existing = current.drafts.firstOrNull { it.id == current.activeId }
        val now = System.currentTimeMillis()
        val updated = (existing ?: emptyCvDocument()).copy(
            name = current.draft.contact.fullName.ifBlank { existing?.name ?: "CV Saya" },
            content = current.draft,
            templateId = current.templateId,
            updatedAt = now
        )
        draftStore.save(updated)
        _state.value = current.copy(
            activeId = updated.id,
            drafts = draftStore.listDraftIds().mapNotNull { draftStore.load(it) },
            saveState = "saved",
            message = "CV berhasil disimpan."
        )
        return true
    }


    fun deleteDocument(id: String) {
        draftStore.delete(id)
        val drafts = draftStore.listDraftIds().mapNotNull { draftStore.load(it) }
        val nextActive = drafts.firstOrNull { it.id != id } ?: drafts.firstOrNull()
        _state.value = _state.value.copy(
            drafts = drafts,
            activeId = nextActive?.id,
            draft = nextActive?.content ?: CvContent(),
            templateId = nextActive?.templateId ?: CvBuilderConstants.TEMPLATE_STANDARD,
            latexStyle = nextActive?.content?.latexStyle ?: "classic",
            style = nextActive?.content?.style ?: CvStyle(),
            saveState = null
        )
    }

    fun runAtsCheck(
        jobTitle: String,
        company: String,
        jobDescription: String,
        explicitKeywords: List<String> = emptyList()
    ) {
        val current = _state.value
        _state.value = current.copy(isAnalyzing = true, ats = null, keyword = null, keywordJobDescription = jobDescription.ifBlank { null })
        viewModelScope.launch {
            val report = CvAtsChecker.check(current.draft, current.latexStyle)
            val keywordReport = CvAtsChecker.keywordMatch(jobDescription, current.draft, explicitKeywords)
            _state.value = _state.value.copy(
                isAnalyzing = false,
                ats = report,
                keyword = keywordReport
            )
        }
    }

    fun generateCoverLetter(jobTitle: String, company: String): CvCoverLetter {
        val current = _state.value
        val letter = CvCoverLetterGenerator.generate(current.draft, jobTitle, company)
        _state.value = current.copy(coverLetter = letter)
        return letter
    }

    fun clearMessages() {
        _state.value = _state.value.copy(message = null)
    }
}
