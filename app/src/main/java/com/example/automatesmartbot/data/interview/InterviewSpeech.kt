package com.example.automatesmartbot.data.interview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * TTS untuk Latihan Terpandu: Bahasa Indonesia, satu utterance utuh per
 * pertanyaan/respons, ucapan sebelumnya selalu dibatalkan dulu.
 * Voice dipilih yang paling natural di perangkat (Google / Natural / Neural).
 */
class GuidedTts(context: Context) {
    private var onDone: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun postDone(cb: (() -> Unit)?) {
        if (cb == null) return
        mainHandler.post { cb.invoke() }
    }
    private var ready = false
    private var pendingText: String? = null

    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            applyVoice()
            pendingText?.let { text ->
                pendingText = null
                speakNow(text)
            }
        }
    }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                val cb = onDone
                onDone = null
                postDone(cb)
            }

            override fun onError(utteranceId: String?) {
                val cb = onDone
                onDone = null
                postDone(cb)
            }
        })
    }

    private fun applyVoice() {
        val locale = Locale.forLanguageTag("id-ID")
        tts.language = locale
        // Pilih voice Bahasa Indonesia paling natural yang terpasang.
        val idVoices = tts.voices
            ?.filter { it.locale.language == "in" || it.locale.language == "id" }
            .orEmpty()
        val best = idVoices.maxByOrNull { voice ->
            var score = 0
            val name = voice.name.lowercase()
            if ("google" in name) score += 3
            if ("natural" in name) score += 2
            if ("neural" in name) score += 2
            if (voice.locale == locale) score += 1
            score
        }
        if (best != null) tts.voice = best
        tts.setSpeechRate(0.98f)
        tts.setPitch(1.0f)
    }

    /** Batalkan ucapan sebelumnya, lalu bacakan [text] sebagai satu utterance utuh. */
    fun speak(text: String, done: (() -> Unit)? = null) {
        if (text.isBlank()) {
            done?.invoke()
            return
        }
        onDone = done
        if (!ready) {
            pendingText = text
            return
        }
        speakNow(text)
    }

    private fun speakNow(text: String) {
        tts.stop()
        applyVoice()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "karirmate-guided-${System.nanoTime()}")
    }

    fun stop() {
        onDone = null
        pendingText = null
        runCatching { tts.stop() }
    }

    fun shutdown() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }
}

/**
 * Anti-duplikasi jawaban suara (pelajaran dari bug web: hasil interim yang
 * menumpuk jadi "saya saya saya...").
 * Aturan: hanya hasil FINAL yang disimpan; hasil interim tidak pernah di-append.
 * [mergeFinal] menggabungkan satu hasil final baru ke teks yang sudah ada
 * dengan deteksi tumpang-tindih kata; [cleanRepeats] membersihkan pengulangan
 * kata/frasa saat jawaban dikirim.
 */
fun mergeFinalResults(existing: String, newFinal: String): String {
    val base = existing.trim()
    val add = cleanRepeats(newFinal.trim())
    if (add.isBlank()) return base
    if (base.isBlank()) return add
    if (base.contains(add, ignoreCase = true)) return base
    if (add.contains(base, ignoreCase = true)) return add
    // Tumpang-tindih: ekor base == kepala add (maks 6 kata).
    val baseWords = base.split(Regex("\\s+"))
    val addWords = add.split(Regex("\\s+"))
    val maxOverlap = minOf(6, baseWords.size, addWords.size)
    for (n in maxOverlap downTo 1) {
        val tail = baseWords.takeLast(n).joinToString(" ")
        val head = addWords.take(n).joinToString(" ")
        if (tail.equals(head, ignoreCase = true)) {
            return (baseWords + addWords.drop(n)).joinToString(" ")
        }
    }
    return "$base $add"
}

/** Hapus pengulangan: "saya saya saya" -> "saya"; frasa berulang berurutan -> sekali. */
fun cleanRepeats(text: String): String {
    var words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
    if (words.isEmpty()) return ""
    // 1) Runtun kata sama >= 3x -> 1x (dua kali dipertahankan: "sangat sangat").
    val collapsed = mutableListOf<String>()
    var run = 1
    for (i in words.indices) {
        if (i > 0 && words[i].equals(words[i - 1], ignoreCase = true)) run++ else run = 1
        if (run <= 2) collapsed.add(words[i])
    }
    words = collapsed
    // 2) Frasa berulang berurutan (2..6 kata) -> sekali. Ulangi sampai stabil.
    repeat(10) {
        var changed = false
        var n = 6
        while (n >= 2 && !changed) {
            var i = 0
            while (i + 2 * n <= words.size) {
                val a = words.subList(i, i + n).joinToString(" ").lowercase()
                val b = words.subList(i + n, i + 2 * n).joinToString(" ").lowercase()
                if (a == b) {
                    repeat(n) { words.removeAt(i + n) }
                    changed = true
                    break
                }
                i++
            }
            n--
        }
        if (!changed) return words.joinToString(" ")
    }
    return words.joinToString(" ")
}
