package com.example.automatesmartbot.data.cv

enum class CvIssueSeverity {
    FAIL,
    WARN,
    INFO
}

data class CvIssue(
    val id: String,
    val severity: String,
    val title: String,
    val detail: String,
    val step: String? = null
)

data class CvPass(
    val id: String,
    val title: String
)

data class CvAtsStats(
    val words: Int,
    val bullets: Int,
    val quantified: Int,
    val weak: Int
)

data class CvAtsReport(
    val score: Int,
    val ready: Boolean,
    val issues: List<CvIssue>,
    val passes: List<CvPass>,
    val stats: CvAtsStats
)

data class CvKeywordReport(
    val matched: List<String>,
    val missing: List<String>,
    val coverage: Double
)

object CvAtsChecker {
    private val WEAK_STARTS = listOf(
        "responsible for",
        "worked on",
        "worked with",
        "helped",
        "assisted",
        "duties included",
        "tasked with",
        "participated in",
        "involved in",
        "was ",
        "i ",
        "my ",
        "we "
    )

    private val SPECIAL: Regex = Regex("[←-⇿☀-➿★☆✓✔➤►▪▸]")

    private val EMAIL: Regex = Regex("^[^\s@]+@[^\s@]+\\.[^\s@]+$")
    private val PHONE_DIGITS: Regex = Regex("\\d")
    private val STOPWORDS = setOf(
        "the", "and", "for", "with", "from", "that", "this", "your", "you", "will", "have", "been",
        "were", "are", "not", "but", "all", "can", "may", "she", "him", "her", "his", "our", "out",
        "into", "over", "after", "before", "under", "while", "when", "where", "which", "who", "whom"
    )

    fun check(resume: CvContent, latexStyle: String = "classic"): CvAtsReport {
        val issues = mutableListOf<CvIssue>()
        val passes = mutableListOf<CvPass>()

        passes.add(CvPass("A1", "Single-column layout"))
        passes.add(CvPass("A2", "No tables or text boxes"))
        passes.add(CvPass("A4", "Contact details in the page body, not a header"))
        passes.add(CvPass("A5", "No images, icons, or skill bars"))
        passes.add(CvPass("A6", "Standard section headings (Work Experience, Education, Skills)"))
        passes.add(CvPass("A7", "Text-selectable PDF with Unicode mapping"))
        passes.add(CvPass("B3", "Standard, embedded fonts at 10-12pt"))
        passes.add(CvPass("B4", "Plain bullet characters"))
        passes.add(CvPass("B6", "Skills listed as labelled lines"))

        if (latexStyle == "columns") {
            issues.add(
                CvIssue(
                    id = "L1",
                    severity = CvIssueSeverity.WARN.name,
                    title = "The Two-column LaTeX style is not parser-safe",
                    detail = "PDF and DOCX exports are single-column and fine. The LaTeX export uses a sidebar that ATS portals read out of order. Switch to Classic, Bold or Banking.",
                    step = "template"
                )
            )
        }

        val contact = resume.contact
        if (contact.fullName.isBlank()) {
            issues.add(CvIssue("C1", CvIssueSeverity.FAIL.name, "Missing name", "The parser builds the whole profile around it.", "basics"))
        }
        if (contact.email.isBlank()) {
            issues.add(CvIssue("C2", CvIssueSeverity.FAIL.name, "Missing email", "No email, no contact record. They can't get back to you.", "basics"))
        } else if (!EMAIL.matches(contact.email.trim())) {
            issues.add(CvIssue("C2b", CvIssueSeverity.WARN.name, "Email doesn't look valid", "Email \"${contact.email}\" won't match an email pattern.", "basics"))
        }
        if (contact.phone.isBlank()) {
            issues.add(CvIssue("C3", CvIssueSeverity.WARN.name, "Missing phone number", "Most portals have a required phone field the parser tries to fill in for you.", "basics"))
        } else if (PHONE_DIGITS.replace(Regex("\\D"), "").length < 10) {
            issues.add(CvIssue("C3b", CvIssueSeverity.WARN.name, "Phone number looks short", "Include the area code (and country code if applying abroad).", "basics"))
        }
        if (contact.location.isBlank()) {
            issues.add(CvIssue("C4", CvIssueSeverity.WARN.name, "Missing location", "Recruiters filter by city or country. A city or country is enough.", "basics"))
        }
        if (contact.fullName.isNotBlank() && contact.email.isNotBlank() && contact.phone.isNotBlank() && contact.location.isNotBlank()) {
            passes.add(CvPass("C0", "Complete contact block"))
        }

        if (!sectionHasContent(resume, "experience") && !sectionHasContent(resume, "education")) {
            issues.add(CvIssue("S1", CvIssueSeverity.FAIL.name, "No experience or education", "With neither, the parser has nothing to score you on.", "experience"))
        }
        if (!sectionHasContent(resume, "skills")) {
            issues.add(CvIssue("S2", CvIssueSeverity.WARN.name, "No skills section", "Keyword matching leans on it heavily. Two to four grouped lines is plenty.", "skills"))
        } else {
            passes.add(CvPass("S2", "Skills section present"))
        }

        var words = 0
        var bullets = 0
        var quantified = 0
        var weak = 0
        val weakExamples = mutableListOf<String>()
        val longExamples = mutableListOf<String>()
        val specialExamples = mutableListOf<String>()

        fun scanBullets(list: List<String>) {
            for (b in list) {
                val t = b.trim()
                if (t.isEmpty()) continue
                bullets++
                val w = t.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                words += w
                if (Regex("\\d").containsMatchIn(t)) quantified++
                val lower = t.lowercase()
                if (WEAK_STARTS.any { lower.startsWith(it) }) {
                    weak++
                    if (weakExamples.size < 3) weakExamples.add(t.take(60))
                }
                if (w > 38 && longExamples.size < 3) longExamples.add(t.take(60))
                if (SPECIAL.containsMatchIn(t) && specialExamples.size < 3) specialExamples.add(t.take(40))
            }
        }

        if (resume.summary.isNotBlank()) {
            words += resume.summary.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        }

        var noLocation = 0
        resume.experience.forEachIndexed { index, e ->
            val missing = mutableListOf<String>()
            if (e.title.isBlank()) missing.add("title")
            if (e.company.isBlank()) missing.add("company")
            if (e.start.isBlank()) missing.add("start date")
            if (!e.current && e.end.isBlank()) missing.add("end date")
            if (e.location.isBlank()) {
                if (missing.isNotEmpty()) missing.add("location")
                else noLocation++
            }
            if (missing.isNotEmpty()) {
                val isFail = missing.contains("title") || missing.contains("company") || missing.contains("start date")
                issues.add(
                    CvIssue(
                        id = "E${index}m",
                        severity = if (isFail) CvIssueSeverity.FAIL.name else CvIssueSeverity.WARN.name,
                        title = "Role ${index + 1}: missing ${missing.joinToString(", ")}",
                        detail = "Parsers file each job under title, employer, location and dates. Lose one and the whole entry can go with it.",
                        step = "experience"
                    )
                )
            }
            if (e.bullets.none { it.isNotBlank() }) {
                issues.add(
                    CvIssue(
                        id = "E${index}b",
                        severity = CvIssueSeverity.WARN.name,
                        title = "Role ${e.title.ifBlank { "Role " + (index + 1) }}: no bullets",
                        detail = "Two to five bullets per role. Say what changed because you were there.",
                        step = "experience"
                    )
                )
            }
            if (e.start.isNotBlank() && e.end.isNotBlank() && !e.current && e.end < e.start) {
                issues.add(
                    CvIssue(
                        id = "E${index}d",
                        severity = CvIssueSeverity.FAIL.name,
                        title = "Role ${e.title.ifBlank { "Role " + (index + 1) }}: end date before start date",
                        detail = "The tenure maths breaks.",
                        step = "experience"
                    )
                )
            }
            scanBullets(e.bullets)
        }

        if (noLocation > 0) {
            issues.add(
                CvIssue(
                    id = "E-location",
                    severity = CvIssueSeverity.INFO.name,
                    title = if (noLocation == 1) "One role has no location" else "${noLocation} roles have no location",
                    detail = "Some portals fill a job's city from it. Remote is enough.",
                    step = "experience"
                )
            )
        }

        val starts = resume.experience.map { it.start }.filter { it.isNotBlank() }.sorted()
        if (starts.size > 1) {
            var outOfOrder = false
            for (i in starts.indices) {
                if (i > 0 && starts[i] > starts[i - 1]) {
                    outOfOrder = true
                    break
                }
            }
            if (outOfOrder) {
                issues.add(CvIssue("E-order", CvIssueSeverity.WARN.name, "Experience isn't in reverse-chronological order", "Most recent role first. Parsers and recruiters both assume it.", "experience"))
            } else {
                passes.add(CvPass("E-order", "Reverse-chronological order"))
            }
        }

        resume.education.forEachIndexed { index, e ->
            if (e.school.isBlank() || e.degree.isBlank()) {
                issues.add(
                    CvIssue(
                        id = "D${index}",
                        severity = CvIssueSeverity.WARN.name,
                        title = "Education ${index + 1}: missing ${if (e.school.isBlank()) "school" else "degree"}",
                        detail = "Degree parsers look for both.",
                        step = "education"
                    )
                )
            }
            scanBullets(e.details)
        }
        resume.projects.forEach { p -> scanBullets(p.bullets) }

        if (weak > 0) {
            issues.add(
                CvIssue(
                    id = "Q1",
                    severity = CvIssueSeverity.WARN.name,
                    title = "${weak} bullet${if (weak > 1) "s" else ""} open with weak phrasing",
                    detail = "Lead with what you did, not that you were \"responsible for\" it.",
                    step = "experience"
                )
            )
        }
        if (bullets >= 3 && quantified / bullets < 0.4) {
            issues.add(CvIssue("Q2", CvIssueSeverity.INFO.name, "Only $quantified of $bullets bullets have a number", "Aim for half or more. Latency, revenue, users, time saved, team size.", "experience"))
        } else if (bullets >= 3) {
            passes.add(CvPass("Q2", "Most bullets are quantified"))
        }
        if (longExamples.isNotEmpty()) {
            issues.add(
                CvIssue(
                    id = "Q3",
                    severity = CvIssueSeverity.INFO.name,
                    title = "Some bullets run past two lines",
                    detail = "Keep bullets under about 35 words.",
                    step = "experience"
                )
            )
        }
        if (specialExamples.isNotEmpty()) {
            issues.add(CvIssue("B5", CvIssueSeverity.WARN.name, "Arrows, stars, or emoji in text", "Older parsers turn them into junk characters mid-sentence.", "experience"))
        } else {
            passes.add(CvPass("B5", "No special characters"))
        }

        val years = yearsOfExperience(resume)
        val wordCap = if (years >= 8 || resume.templateId == "senior") 1100 else 620
        if (words > wordCap) {
            issues.add(
                CvIssue(
                    id = "P1",
                    severity = CvIssueSeverity.INFO.name,
                    title = "Roughly $words words, which is probably over ${if (years >= 8) "two pages" else "one page"}",
                    detail = "Under about eight years of experience, one page is the norm.",
                    step = "experience"
                )
            )
        }

        var score = 100
        for (issue in issues) {
            score -= when (issue.severity) {
                CvIssueSeverity.FAIL.name -> 18
                CvIssueSeverity.WARN.name -> 7
                else -> 3
            }
        }
        score = score.coerceIn(0, 100)
        val ready = score >= 85 && issues.none { it.severity == CvIssueSeverity.FAIL.name }

        val severityOrder = mapOf(
            CvIssueSeverity.FAIL.name to 0,
            CvIssueSeverity.WARN.name to 1,
            CvIssueSeverity.INFO.name to 2
        )
        val sortedIssues = issues.sortedBy { severityOrder[it.severity] ?: 2 }

        return CvAtsReport(
            score = score,
            ready = ready,
            issues = sortedIssues,
            passes = passes,
            stats = CvAtsStats(words = words, bullets = bullets, quantified = quantified, weak = weak)
        )
    }

    fun keywordMatch(
        jobDescription: String,
        resume: CvContent,
        explicitKeywords: List<String> = emptyList()
    ): CvKeywordReport? {
        val jd = jobDescription.trim()
        val normalizedExplicit = explicitKeywords.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(100)
        if (normalizedExplicit.isEmpty() && jd.length < 40) return null
        val terms = normalizedExplicit.ifEmpty { suggestKeywords(jd) }
        if (terms.isEmpty()) return null
        val resumeText = resumeToText(resume)
        val matched = terms.filter { containsKeyword(resumeText, it) }
        val missing = terms.filterNot { containsKeyword(resumeText, it) }
        val total = matched.size + missing.size
        return CvKeywordReport(
            matched = matched,
            missing = missing,
            coverage = if (total == 0) 0.0 else matched.size.toDouble() / total
        )
    }

    fun suggestKeywords(jobDescription: String): List<String> {
        val frequencies = mutableMapOf<String, Int>()
        for (raw in jobDescription.split(Regex("[,;\\n]")) {
            val words = raw.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
            for (word in words) {
                val clean = word.trim()
                if (clean.length < 3 || STOPWORDS.contains(clean)) continue
                frequencies[clean] = (frequencies[clean] ?: 0) + 1
            }
        }
        return frequencies.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
            .distinct()
            .take(40)
    }

    fun containsKeyword(text: String, term: String): Boolean {
        val normalizedText = text.lowercase().replace(Regex("\\s+"), " ").trim()
        val normalizedTerm = term.lowercase().replace(Regex("\\s+"), " ").trim()
        if (normalizedTerm.isEmpty()) return false
        return normalizedText.contains(normalizedTerm)
    }

    fun resumeToText(resume: CvContent): String {
        val parts = mutableListOf<String>()
        parts.add(resume.contact.fullName)
        parts.add(resume.contact.headline)
        parts.add(resume.summary)
        resume.experience.forEach { e ->
            parts.add(e.title)
            parts.add(e.company)
            parts.addAll(e.bullets)
        }
        resume.education.forEach { e ->
            parts.add(e.school)
            parts.add(e.degree)
            parts.add(e.field)
            parts.addAll(e.details)
        }
        resume.skills.forEach { g ->
            parts.add(g.category)
            parts.addAll(g.items)
        }
        resume.projects.forEach { p ->
            parts.add(p.name)
            parts.add(p.tech)
            parts.addAll(p.bullets)
        }
        resume.certifications.forEach { c ->
            parts.add(c.name)
            parts.add(c.issuer)
        }
        return parts.filter { it.isNotBlank() }.joinToString("\n")
    }

    private fun sectionHasContent(resume: CvContent, id: String): Boolean {
        return when (id) {
            "summary" -> resume.summary.isNotBlank()
            "experience" -> resume.experience.isNotEmpty()
            "education" -> resume.education.isNotEmpty()
            "skills" -> resume.skills.any { it.items.isNotEmpty() }
            "projects" -> resume.projects.isNotEmpty()
            "certifications" -> resume.certifications.isNotEmpty()
            else -> false
        }
    }

    private fun yearsOfExperience(resume: CvContent): Int {
        val starts = resume.experience.map { it.start }.filter { it.isNotBlank() }.sorted()
        if (starts.isEmpty()) return 0
        val first = starts.first().take(4).toIntOrNull() ?: return 0
        return java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - first
    }
}
