package com.example.automatesmartbot.data.cv

data class CvContact(
    val fullName: String = "",
    val headline: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedin: String = "",
    val github: String = "",
    val photoPath: String = ""
)

data class CvExperience(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val start: String = "",
    val end: String = "",
    val current: Boolean = false,
    val bullets: List<String> = emptyList()
)

data class CvEducation(
    val id: String = "",
    val school: String = "",
    val degree: String = "",
    val field: String = "",
    val location: String = "",
    val start: String = "",
    val end: String = "",
    val gpa: String = "",
    val details: List<String> = emptyList()
)

data class CvSkillGroup(
    val id: String = "",
    val category: String = "",
    val items: List<String> = emptyList()
)

data class CvProject(
    val id: String = "",
    val name: String = "",
    val link: String = "",
    val tech: String = "",
    val start: String = "",
    val end: String = "",
    val bullets: List<String> = emptyList()
)

data class CvCertification(
    val id: String = "",
    val name: String = "",
    val issuer: String = "",
    val date: String = "",
    val link: String = ""
)

data class CvStyle(
    val font: String = "sans",
    val size: String = "10.5",
    val margins: String = "normal",
    val accent: String = "none",
    val leading: String = "normal",
    val css: String = ""
)

data class CvExtraSection(val id: String = "", val title: String = "", val text: String = "")

data class CvContent(
    val contact: CvContact = CvContact(),
    val summary: String = "",
    val extras: List<CvExtraSection> = emptyList(),
    val experience: List<CvExperience> = emptyList(),
    val education: List<CvEducation> = emptyList(),
    val skills: List<CvSkillGroup> = emptyList(),
    val projects: List<CvProject> = emptyList(),
    val certifications: List<CvCertification> = emptyList(),
    val style: CvStyle = CvStyle(),
    val latexStyle: String = "classic",
    val sectionOrder: List<String> = listOf(
        "summary",
        "experience",
        "education",
        "skills",
        "projects",
        "certifications"
    )
)

data class CvDocument(
    val id: String,
    val name: String,
    val templateId: String,
    val content: CvContent,
    val createdAt: Long,
    val updatedAt: Long
)

object CvBuilderConstants {
    const val TEMPLATE_STANDARD = "standard"
    const val TEMPLATE_STUDENT = "student"
    const val TEMPLATE_COMBINATION = "combination"
    const val TEMPLATE_SENIOR = "senior"

    val HTML_TEMPLATE_IDS: List<String> get() = CvTemplates.all.map { it.id }

    val SECTION_ORDER: List<String> = listOf(
        "summary",
        "experience",
        "education",
        "skills",
        "projects",
        "certifications"
    )
}

fun newCvId(): String = "cv_" + java.util.UUID.randomUUID().toString()

fun emptyCvDocument(
    name: String = "CV Baru",
    templateId: String = CvBuilderConstants.TEMPLATE_STANDARD
): CvDocument = CvDocument(
    id = newCvId(),
    name = name,
    templateId = templateId,
    content = CvContent(),
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

fun CvContent.hasUserText(): Boolean = listOf(contact.fullName, contact.headline, contact.email, contact.phone,
    contact.location, contact.website, contact.linkedin, contact.github, summary).any { it.isNotBlank() } ||
    experience.any { it.title.isNotBlank() || it.company.isNotBlank() || it.bullets.any(String::isNotBlank) } ||
    education.any { it.school.isNotBlank() || it.degree.isNotBlank() || it.details.any(String::isNotBlank) } ||
    skills.any { it.items.any(String::isNotBlank) } || projects.any { it.name.isNotBlank() || it.bullets.any(String::isNotBlank) } ||
    certifications.any { it.name.isNotBlank() } || extras.any { it.text.isNotBlank() }
