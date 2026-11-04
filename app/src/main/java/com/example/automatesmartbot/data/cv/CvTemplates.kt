package com.example.automatesmartbot.data.cv

enum class CvLayout { CLASSIC, CENTERED, SIDEBAR_LEFT, SIDEBAR_RIGHT, TIMELINE, ACADEMIC, BANNER, EDITORIAL, COMPACT, SKILLS_FIRST, EXECUTIVE, MONOGRAM, PORTFOLIO, RULED }
data class CvTemplate(val id: String, val name: String, val category: String, val layout: CvLayout, val accent: Int, val supportsPhoto: Boolean = false)

object CvTemplates {
    // A curated catalog: each entry changes geometry or section placement, not just color.
    val all = listOf(
        CvTemplate("ats_01", "ATS Klasik", "ATS", CvLayout.CLASSIC, 0xFF172744.toInt()),
        CvTemplate("ats_02", "ATS Terpusat", "ATS", CvLayout.CENTERED, 0xFF172744.toInt()),
        CvTemplate("professional_01", "Profesional Sidebar", "Profesional", CvLayout.SIDEBAR_LEFT, 0xFF193B60.toInt(), true),
        CvTemplate("professional_02", "Profesional Kronologi", "Profesional", CvLayout.TIMELINE, 0xFF193B60.toInt(), true),
        CvTemplate("modern_01", "Modern Panel Kanan", "Modern", CvLayout.SIDEBAR_RIGHT, 0xFF006672.toInt(), true),
        CvTemplate("modern_02", "Modern Banner", "Modern", CvLayout.BANNER, 0xFF006672.toInt(), true),
        CvTemplate("minimal_01", "Minimal Ringkas", "Minimalis", CvLayout.COMPACT, 0xFF34434A.toInt()),
        CvTemplate("minimal_02", "Minimal Editorial", "Minimalis", CvLayout.EDITORIAL, 0xFF34434A.toInt()),
        CvTemplate("graduate_01", "Lulusan Akademik", "Fresh Graduate", CvLayout.ACADEMIC, 0xFF245486.toInt(), true),
        CvTemplate("graduate_02", "Lulusan Keterampilan", "Fresh Graduate", CvLayout.SKILLS_FIRST, 0xFF245486.toInt()),
        CvTemplate("executive_01", "Eksekutif Profil", "Eksekutif", CvLayout.EXECUTIVE, 0xFF50365F.toInt(), true),
        CvTemplate("executive_02", "Eksekutif Garis", "Eksekutif", CvLayout.RULED, 0xFF50365F.toInt()),
        CvTemplate("creative_01", "Kreatif Monogram", "Kreatif", CvLayout.MONOGRAM, 0xFF943D24.toInt(), true),
        CvTemplate("creative_02", "Kreatif Portofolio", "Kreatif", CvLayout.PORTFOLIO, 0xFF943D24.toInt(), true)
    )
    private val generatedCatalog: List<CvTemplate> = buildList {
        for (number in 1..15) {
            val id = "ats_" + number.toString().padStart(2, '0')
            if (all.none { it.id == id }) add(CvTemplate(id, "ATS " + number.toString().padStart(2, '0'), "ATS", listOf(CvLayout.CLASSIC, CvLayout.CENTERED, CvLayout.COMPACT, CvLayout.RULED)[(number - 1) % 4], 0xFFFF6B2B.toInt(), false))
        }
        for (number in 1..15) {
            val id = "professional_" + number.toString().padStart(2, '0')
            if (all.none { it.id == id }) add(CvTemplate(id, "Profesional " + number.toString().padStart(2, '0'), "Profesional", listOf(CvLayout.SIDEBAR_LEFT, CvLayout.TIMELINE, CvLayout.EDITORIAL, CvLayout.RULED)[(number - 1) % 4], 0xFFFF6B2B.toInt(), true))
        }
        for (number in 1..12) {
            val id = "modern_" + number.toString().padStart(2, '0')
            if (all.none { it.id == id }) add(CvTemplate(id, "Modern " + number.toString().padStart(2, '0'), "Modern", listOf(CvLayout.SIDEBAR_RIGHT, CvLayout.BANNER, CvLayout.MONOGRAM, CvLayout.EDITORIAL)[(number - 1) % 4], 0xFFFF6B2B.toInt(), true))
        }
        for (number in 1..10) {
            val id = "minimal_" + number.toString().padStart(2, '0')
            if (all.none { it.id == id }) add(CvTemplate(id, "Minimalis " + number.toString().padStart(2, '0'), "Minimalis", listOf(CvLayout.COMPACT, CvLayout.EDITORIAL, CvLayout.CENTERED)[(number - 1) % 3], 0xFFFF6B2B.toInt(), false))
        }
        for (number in 1..8) {
            val id = "graduate_" + number.toString().padStart(2, '0')
            if (all.none { it.id == id }) add(CvTemplate(id, "Fresh Graduate " + number.toString().padStart(2, '0'), "Fresh Graduate", listOf(CvLayout.ACADEMIC, CvLayout.SKILLS_FIRST, CvLayout.CLASSIC)[(number - 1) % 3], 0xFFFF6B2B.toInt(), true))
        }
        for (number in 1..7) {
            val id = "executive_" + number.toString().padStart(2, '0')
            if (all.none { it.id == id }) add(CvTemplate(id, "Eksekutif " + number.toString().padStart(2, '0'), "Eksekutif", listOf(CvLayout.EXECUTIVE, CvLayout.RULED, CvLayout.TIMELINE)[(number - 1) % 3], 0xFFFF6B2B.toInt(), true))
        }
        for (number in 1..8) {
            val id = "creative_" + number.toString().padStart(2, '0')
            if (all.none { it.id == id }) add(CvTemplate(id, "Kreatif " + number.toString().padStart(2, '0'), "Kreatif", listOf(CvLayout.MONOGRAM, CvLayout.PORTFOLIO, CvLayout.BANNER)[(number - 1) % 3], 0xFFFF6B2B.toInt(), true))
        }
    }
    val catalog: List<CvTemplate> = all + generatedCatalog
    // Keep all original templates FREE; distribute remaining FREE slots across categories.
    private val freeQuota = mapOf("ATS" to 5, "Profesional" to 5, "Modern" to 4, "Minimalis" to 4, "Fresh Graduate" to 3, "Eksekutif" to 3, "Kreatif" to 3)
    val freeIds: Set<String> = buildSet {
        addAll(all.map { it.id })
        freeQuota.forEach { (category, count) ->
            addAll(catalog.filter { it.category == category }.take(count).map { it.id })
        }
    }
    fun isProTemplate(id: String): Boolean = catalog.any { it.id == id } && id !in freeIds
    fun canUse(id: String, verifiedPro: Boolean): Boolean = catalog.any { it.id == id } && (!isProTemplate(id) || verifiedPro)
    /** Old IDs remain readable without deleting or migrating saved CVs. */
    fun resolve(id: String): CvTemplate = catalog.firstOrNull { it.id == id } ?: when (id) {
        "student" -> all.first { it.layout == CvLayout.ACADEMIC }
        "combination" -> all.first { it.layout == CvLayout.SKILLS_FIRST }
        "senior" -> all.first { it.layout == CvLayout.EXECUTIVE }
        else -> {
            val group = all.filter { it.id.substringBefore('_') == id.substringBefore('_') }
            if (group.isEmpty()) all.first() else group[((id.substringAfterLast('_').toIntOrNull() ?: 1) - 1).coerceAtLeast(0) % group.size]
        }
    }
    val sample = CvContent(
        contact = CvContact(fullName = "Dina Pratama", headline = "Staf Administrasi", email = "dina@example.com", phone = "0812 0000 0000", location = "Jakarta"),
        summary = "Contoh CV untuk melihat desain. Berpengalaman dalam administrasi dan pengelolaan dokumen.",
        experience = listOf(CvExperience(title = "Staf Administrasi", company = "Perusahaan Contoh", start = "2023", end = "2025", bullets = listOf("Mengelola arsip dan menyusun laporan administrasi mingguan."))),
        education = listOf(CvEducation(school = "Universitas Contoh", degree = "S1", field = "Manajemen", start = "2019", end = "2023")),
        skills = listOf(CvSkillGroup(category = "Administrasi", items = listOf("Pengelolaan dokumen", "Microsoft Excel", "Komunikasi")))
    )
}
