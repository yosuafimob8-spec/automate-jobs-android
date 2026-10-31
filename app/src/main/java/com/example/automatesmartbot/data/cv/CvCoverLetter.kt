package com.example.automatesmartbot.data.cv

data class CvCoverLetter(
    val id: String,
    val jobTitle: String,
    val company: String,
    val content: String,
    val createdAt: Long
)

object CvCoverLetterGenerator {
    fun generate(resume: CvContent, jobTitle: String, company: String): CvCoverLetter {
        val subject = "Application: ${jobTitle.trim()} - ${company.trim()}".trim()
        val sb = StringBuilder()
        sb.append("Subject: ").append(subject).append("\n\n")
        sb.append("Dear Hiring Manager,\n\n")
        sb.append("I am writing to express my interest in the ${jobTitle.trim()} position")
        if (company.trim().isNotEmpty()) {
            sb.append(" at ${company.trim()}")
        }
        sb.append(".\n\n")

        val background = StringBuilder()
        resume.summary.takeIf { it.isNotBlank() }?.let {
            background.append("My background includes: ").append(it.trim()).append("\n\n")
        }
        val experiences = resume.experience.filter { it.title.isNotBlank() || it.company.isNotBlank() }
        if (experiences.isNotEmpty()) {
            val first = experiences.first()
            background.append("My background includes experience as ${first.title.ifBlank { "the role" }}")
            if (first.company.isNotBlank()) {
                background.append(" at ${first.company}")
            }
            background.append(".\n\n")
            first.bullets.filter { it.isNotBlank() }.take(3).forEach { bullet ->
                background.append(bullet.trim()).append("\n\n")
            }
        }

        if (background.isNotBlank()) {
            sb.append(background)
        }

        sb.append("Thank you for your consideration. I would welcome the chance to discuss how my background aligns with your needs.\n")
        sb.append("Sincerely,\n")

        return CvCoverLetter(
            id = newCvId(),
            jobTitle = jobTitle.trim(),
            company = company.trim(),
            content = sb.toString().trim(),
            createdAt = System.currentTimeMillis()
        )
    }
}
