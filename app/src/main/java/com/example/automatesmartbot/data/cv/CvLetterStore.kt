package com.example.automatesmartbot.data.cv

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import java.io.File

class CvLetterStore(context: Context) {
    private val dir = File(context.filesDir, "cv_letters").apply { mkdirs() }
    fun save(letter: CvCoverLetter) {
        require(letter.id.matches(Regex("[A-Za-z0-9_-]+")))
        val file = AtomicFile(File(dir, "${letter.id}.json"))
        val stream = file.startWrite()
        try { stream.write(Gson().toJson(letter).toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
    fun list(): List<CvCoverLetter> = dir.listFiles()?.filter { it.extension == "json" }?.mapNotNull {
        runCatching { Gson().fromJson(it.readText(), CvCoverLetter::class.java) }.getOrNull()
    }?.sortedByDescending { it.createdAt } ?: emptyList()
}
