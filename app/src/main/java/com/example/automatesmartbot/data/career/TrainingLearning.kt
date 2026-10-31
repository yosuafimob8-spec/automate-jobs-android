package com.example.automatesmartbot.data.career

import android.content.Context
import com.example.automatesmartbot.data.cv.CvCertification
import com.google.gson.Gson
import java.time.LocalDate
import java.util.UUID

data class LearningReference(val title: String, val publisher: String, val standard: String?, val url: String, val checkedOn: String, val scope: String)
data class LearningQuestion(val prompt: String, val options: List<String>, val correct: Int, val explanation: String)
data class LearningModule(val id: String, val title: String, val objective: String, val content: String, val references: List<LearningReference>, val practice: List<LearningQuestion>)
data class LearningCourse(val id: String, val title: String, val description: String, val modules: List<LearningModule>, val simulation: LearningQuestion, val exam: List<LearningQuestion>)
data class LearningCatalog(val version: Int, val courses: List<LearningCourse>) {
    companion object {
        fun load(context: Context): LearningCatalog = Gson().fromJson(context.assets.open("training_catalog.json").bufferedReader().use { it.readText() }, LearningCatalog::class.java)
    }
}
data class LearningAttempt(val score: Int, val completedOn: String, val answers: List<Int>)
data class LearningCertificate(val id: String, val name: String, val course: String, val completedOn: String, val score: Int) {
    fun text() = "SERTIFIKAT PENYELESAIAN PELATIHAN\n\nKarirMate\n\nDiberikan kepada:\n$name\n\nTelah menyelesaikan bidang pelatihan:\n$course\n\nTanggal penyelesaian: $completedOn\nNomor sertifikat internal: $id\nNilai ujian: $score/100\n\nSertifikat ini merupakan bukti penyelesaian pelatihan internal KarirMate, bukan sertifikasi kompetensi resmi BNSP."
    fun asCvCertification() = CvCertification(id = id, name = "Pelatihan Internal KarirMate — $course", issuer = "KarirMate (bukan sertifikasi kompetensi BNSP)", date = completedOn)
}
data class LearningProgress(val completedModules: Set<String> = emptySet(), val simulationCompleted: Boolean = false,
    val answers: Map<String, Int> = emptyMap(), val attempts: List<LearningAttempt> = emptyList(), val certificate: LearningCertificate? = null)

/** Separate namespace preserves the previous courses, answers and certificates intact. */
class LearningStore(context: Context) {
    private val prefs = context.getSharedPreferences("career_learning_v1", 0)
    fun load(id: String): LearningProgress = prefs.getString(id, null)?.let { Gson().fromJson(it, LearningProgress::class.java) } ?: LearningProgress()
    private fun save(id: String, value: LearningProgress): LearningProgress {
        check(prefs.edit().putString(id, Gson().toJson(value)).commit()) { "Progres belum tersimpan. Coba lagi sebelum keluar." }
        return value
    }
    fun answer(course: LearningCourse, key: String, option: Int): LearningProgress {
        val question = when {
            key == "simulation" -> course.simulation
            key.startsWith("exam_") -> course.exam.getOrNull(key.removePrefix("exam_").toIntOrNull() ?: -1)
            else -> course.modules.flatMap { m -> m.practice.mapIndexed { n, q -> "${m.id}_$n" to q } }.firstOrNull { it.first == key }?.second
        }
        require(question != null && option in question.options.indices)
        val old = load(course.id)
        return save(course.id, old.copy(answers = old.answers + (key to option)))
    }
    fun completeModule(course: LearningCourse, index: Int): LearningProgress {
        val old = load(course.id); val module = course.modules[index]
        require(course.modules.take(index).all { it.id in old.completedModules }) { "Selesaikan modul sebelumnya." }
        require(module.practice.indices.all { old.answers["${module.id}_$it"] != null }) { "Jawab latihan modul terlebih dahulu." }
        return save(course.id, old.copy(completedModules = old.completedModules + module.id))
    }
    fun completeSimulation(course: LearningCourse): LearningProgress {
        val old = load(course.id)
        require(course.modules.all { it.id in old.completedModules }) { "Selesaikan semua modul terlebih dahulu." }
        require(old.answers["simulation"] != null) { "Jawab simulasi terlebih dahulu." }
        return save(course.id, old.copy(simulationCompleted = true))
    }
    fun submit(course: LearningCourse, name: String, passScore: Int): LearningProgress {
        val old = load(course.id)
        require(name.isNotBlank()) { "Nama peserta wajib diisi untuk sertifikat." }
        require(old.simulationCompleted && course.modules.all { it.id in old.completedModules }) { "Selesaikan materi dan simulasi terlebih dahulu." }
        val values = course.exam.indices.map { old.answers["exam_$it"] ?: error("Jawab semua soal ujian.") }
        val score = values.zip(course.exam).count { (a, q) -> a == q.correct } * 100 / course.exam.size
        val date = LocalDate.now().toString()
        val certificate = old.certificate ?: if (score >= passScore) LearningCertificate("KM-${UUID.randomUUID()}", name.trim(), course.title, date, score) else null
        return save(course.id, old.copy(attempts = old.attempts + LearningAttempt(score, date, values), certificate = certificate))
    }
}
