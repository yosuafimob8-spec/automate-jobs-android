package com.example.automatesmartbot.data.cv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Stores CV drafts privately in the application's internal storage. */
class CvDraftStore(context: Context) {
    private val directory = File(context.filesDir, "cv_drafts").apply { mkdirs() }

    fun listDraftIds(): List<String> = directory.listFiles()
        ?.filter { it.isFile && it.extension == "json" }
        ?.map { it.nameWithoutExtension }
        ?.sorted() ?: emptyList()

    fun save(document: CvDocument) {
        val destination = fileFor(document.id)
        val temp = File(directory, "${document.id}.tmp")
        val data = JSONObject().apply {
            put("id", document.id)
            put("name", document.name)
            put("templateId", document.templateId)
            put("createdAt", document.createdAt)
            put("updatedAt", document.updatedAt)
            put("content", encodeContent(document.content))
        }
        temp.writeText(data.toString(), Charsets.UTF_8)
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
    }

    fun load(id: String): CvDocument? = runCatching {
        val file = fileFor(id)
        if (!file.isFile) return null
        val json = JSONObject(file.readText(Charsets.UTF_8))
        CvDocument(
            id = json.getString("id"),
            name = json.optString("name", "CV Baru"),
            templateId = json.optString("templateId", CvBuilderConstants.TEMPLATE_STANDARD),
            content = decodeContent(json.getJSONObject("content")),
            createdAt = json.optLong("createdAt"),
            updatedAt = json.optLong("updatedAt")
        )
    }.getOrNull()

    fun delete(id: String) { fileFor(id).delete() }

    private fun fileFor(id: String): File {
        require(id.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid CV ID" }
        return File(directory, "$id.json")
    }

    private fun array(values: List<String>): JSONArray = JSONArray().apply {
        values.forEach { put(it) }
    }

    private fun strings(array: JSONArray?): List<String> =
        if (array == null) emptyList() else (0 until array.length()).map { array.optString(it) }

    private fun encodeContent(c: CvContent): JSONObject = JSONObject().apply {
        put("contact", JSONObject().apply {
            put("fullName", c.contact.fullName)
            put("headline", c.contact.headline)
            put("email", c.contact.email)
            put("phone", c.contact.phone)
            put("location", c.contact.location)
            put("website", c.contact.website)
            put("linkedin", c.contact.linkedin)
            put("github", c.contact.github)
        })
        put("summary", c.summary)
        put("latexStyle", c.latexStyle)
        put("sectionOrder", array(c.sectionOrder))
        put("style", JSONObject().apply {
            put("font", c.style.font)
            put("size", c.style.size)
            put("margins", c.style.margins)
            put("accent", c.style.accent)
            put("leading", c.style.leading)
            put("css", c.style.css)
        })
        put("experience", JSONArray().apply {
            c.experience.forEach { e -> put(JSONObject().apply {
                put("id", e.id); put("title", e.title); put("company", e.company)
                put("location", e.location); put("start", e.start); put("end", e.end)
                put("current", e.current); put("bullets", array(e.bullets))
            }) }
        })
        put("education", JSONArray().apply {
            c.education.forEach { e -> put(JSONObject().apply {
                put("id", e.id); put("school", e.school); put("degree", e.degree)
                put("field", e.field); put("location", e.location); put("start", e.start)
                put("end", e.end); put("gpa", e.gpa); put("details", array(e.details))
            }) }
        })
        put("skills", JSONArray().apply {
            c.skills.forEach { s -> put(JSONObject().apply {
                put("id", s.id); put("category", s.category); put("items", array(s.items))
            }) }
        })
        put("projects", JSONArray().apply {
            c.projects.forEach { p -> put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("link", p.link)
                put("tech", p.tech); put("start", p.start); put("end", p.end)
                put("bullets", array(p.bullets))
            }) }
        })
        put("certifications", JSONArray().apply {
            c.certifications.forEach { cert -> put(JSONObject().apply {
                put("id", cert.id); put("name", cert.name); put("issuer", cert.issuer)
                put("date", cert.date); put("link", cert.link)
            }) }
        })
    }

    private fun decodeContent(j: JSONObject): CvContent {
        val contact = j.optJSONObject("contact") ?: JSONObject()
        val style = j.optJSONObject("style") ?: JSONObject()
        fun <T> decode(name: String, mapper: (JSONObject) -> T): List<T> {
            val items = j.optJSONArray(name) ?: return emptyList()
            return (0 until items.length()).mapNotNull { index ->
                items.optJSONObject(index)?.let(mapper)
            }
        }
        return CvContent(
            contact = CvContact(
                fullName = contact.optString("fullName"),
                headline = contact.optString("headline"),
                email = contact.optString("email"),
                phone = contact.optString("phone"),
                location = contact.optString("location"),
                website = contact.optString("website"),
                linkedin = contact.optString("linkedin"),
                github = contact.optString("github")
            ),
            summary = j.optString("summary"),
            experience = decode("experience") { e -> CvExperience(
                id = e.optString("id"), title = e.optString("title"),
                company = e.optString("company"), location = e.optString("location"),
                start = e.optString("start"), end = e.optString("end"),
                current = e.optBoolean("current"), bullets = strings(e.optJSONArray("bullets"))
            ) },
            education = decode("education") { e -> CvEducation(
                id = e.optString("id"), school = e.optString("school"),
                degree = e.optString("degree"), field = e.optString("field"),
                location = e.optString("location"), start = e.optString("start"),
                end = e.optString("end"), gpa = e.optString("gpa"),
                details = strings(e.optJSONArray("details"))
            ) },
            skills = decode("skills") { s -> CvSkillGroup(
                id = s.optString("id"), category = s.optString("category"),
                items = strings(s.optJSONArray("items"))
            ) },
            projects = decode("projects") { p -> CvProject(
                id = p.optString("id"), name = p.optString("name"),
                link = p.optString("link"), tech = p.optString("tech"),
                start = p.optString("start"), end = p.optString("end"),
                bullets = strings(p.optJSONArray("bullets"))
            ) },
            certifications = decode("certifications") { cert -> CvCertification(
                id = cert.optString("id"), name = cert.optString("name"),
                issuer = cert.optString("issuer"), date = cert.optString("date"),
                link = cert.optString("link")
            ) },
            style = CvStyle(
                font = style.optString("font", "sans"),
                size = style.optString("size", "10.5"),
                margins = style.optString("margins", "normal"),
                accent = style.optString("accent", "none"),
                leading = style.optString("leading", "normal"),
                css = style.optString("css")
            ),
            latexStyle = j.optString("latexStyle", "classic"),
            sectionOrder = if (j.has("sectionOrder")) strings(j.optJSONArray("sectionOrder"))
                else CvBuilderConstants.SECTION_ORDER
        )
    }
}
