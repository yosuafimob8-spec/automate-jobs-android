package com.example.automatesmartbot.data.cv

data class CvContact(
    val fullName: String = "",
    val headline: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val linkedin: String = "",
    val github: String = ""
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

data class CvContent(
    val contact: CvContact = CvContact(),
    val summary: String = "",
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

    val HTML_TEMPLATE_IDS: List<String> = listOf(
        "ats_01",
        "ats_02",
        "ats_03",
        "ats_04",
        "ats_05",
        "ats_06",
        "ats_07",
        "ats_08",
        "modern_01",
        "modern_02",
        "modern_03",
        "modern_04",
        "modern_05",
        "modern_06",
        "graduate_01",
        "graduate_02",
        "graduate_03",
        "graduate_04",
        "executive_01",
        "executive_02",
        "executive_03",
        "executive_04",
        "creative_01",
        "creative_02",
        "creative_03",
        "creative_04",
        "minimal_01",
        "minimal_02",
        "minimal_03",
        "minimal_04",
        "ats_09", "ats_10", "modern_07", "modern_08",
        "graduate_05", "graduate_06", "executive_05", "executive_06",
        "creative_05", "creative_06", "minimal_05", "minimal_06"
    )

    val SECTION_ORDER: List<String> = listOf(
        "summary",
        "experience",
        "education",
        "skills",
        "projects",
        "certifications"
    )
}

fun newCvId(): String = "cv_" + System.currentTimeMillis()

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
