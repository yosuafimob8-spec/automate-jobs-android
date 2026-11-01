package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class InterviewQuestion(val question: String, val hints: List<String>)

private fun interviewQuestions(role: String): List<InterviewQuestion> {
    val common = listOf(
        InterviewQuestion("Ceritakan tentang diri Anda dan pengalaman yang paling relevan.", listOf("pengalaman", "tanggung jawab", "hasil")),
        InterviewQuestion("Mengapa Anda tertarik dengan posisi ini?", listOf("posisi", "perusahaan", "kontribusi")),
        InterviewQuestion("Ceritakan masalah di tempat kerja yang pernah Anda selesaikan.", listOf("masalah", "tindakan", "hasil")),
        InterviewQuestion("Apa kelebihan Anda dan apa yang masih perlu ditingkatkan?", listOf("kelebihan", "perbaikan", "contoh"))
    )
    val specific = when (role) {
        "Admin Warehouse" -> InterviewQuestion("Bagaimana Anda menjaga akurasi stok dan menangani selisih data?", listOf("stok", "cek", "laporan"))
        "Supervisor Logistik" -> InterviewQuestion("Bagaimana Anda mengatur tim saat pengiriman terlambat?", listOf("tim", "prioritas", "pengiriman"))
        "Staff Operasional" -> InterviewQuestion("Bagaimana Anda menentukan prioritas ketika beberapa pekerjaan mendesak datang bersamaan?", listOf("prioritas", "koordinasi", "waktu"))
        "Operator Produksi" -> InterviewQuestion("Apa yang Anda lakukan jika menemukan produk cacat di jalur produksi?", listOf("kualitas", "lapor", "prosedur"))
        else -> InterviewQuestion("Bagaimana Anda menangani pelanggan yang mengajukan komplain?", listOf("mendengar", "solusi", "tindak lanjut"))
    }
    return common + specific
}

private fun answerFeedback(answer: String, question: InterviewQuestion): Pair<Int, String> {
    val words = answer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val hits = question.hints.count { answer.contains(it, ignoreCase = true) }
    val lengthScore = when {
        words.size >= 45 -> 55
        words.size >= 20 -> 40
        words.size >= 8 -> 20
        else -> 5
    }
    val score = (lengthScore + hits * 15).coerceAtMost(100)
    val missing = question.hints.filterNot { answer.contains(it, ignoreCase = true) }
    val advice = if (missing.isEmpty()) {
        "Jawaban sudah menyebut poin penting. Tambahkan contoh nyata dan hasil terukur agar lebih kuat."
    } else {
        "Perkuat jawaban dengan contoh konkret yang menjelaskan: ${missing.joinToString(", ")}. Gunakan alur situasi, tindakan, dan hasil."
    }
    return score to advice
}

@Composable
fun AiInterviewScreen() {
    val roles = listOf("Admin Warehouse", "Supervisor Logistik", "Staff Operasional", "Operator Produksi", "Sales")
    var role by rememberSaveable { mutableStateOf(roles.first()) }
    var started by rememberSaveable { mutableStateOf(false) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var answer by rememberSaveable { mutableStateOf("") }
    var answers by rememberSaveable { mutableStateOf(listOf<String>()) }
    var finished by rememberSaveable { mutableStateOf(false) }
    val questions = remember(role) { interviewQuestions(role) }
    val scores = answers.mapIndexed { i, a -> answerFeedback(a, questions[i]).first }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Latihan Interview Kerja", style = MaterialTheme.typography.headlineSmall)
        Text("Latihan interaktif offline. Penilaian awal menggunakan rubrik otomatis, bukan evaluasi model AI.")
        if (!started) {
            Text("Pilih posisi yang dilamar", style = MaterialTheme.typography.titleMedium)
            roles.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(selected = role == item, onClick = { role = item })
                    Text(item, Modifier.padding(top = 12.dp))
                }
            }
            Button(onClick = {
                started = true
                index = 0
                answer = ""
                answers = emptyList()
                finished = false
            }) { Text("Mulai simulasi") }
        } else if (!finished) {
            LinearProgressIndicator(
                progress = { index.toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Pertanyaan ${index + 1} dari ${questions.size}", style = MaterialTheme.typography.titleMedium)
            Text(questions[index].question)
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Ketik jawaban Anda") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Jelaskan situasi, tindakan, dan hasil. Minimal 8 kata.")
            Button(
                enabled = answer.trim().split(Regex("\\s+")).count { it.isNotBlank() } >= 8,
                onClick = {
                    answers = answers + answer.trim()
                    answer = ""
                    if (index == questions.lastIndex) finished = true else index += 1
                }
            ) { Text(if (index == questions.lastIndex) "Lihat hasil" else "Pertanyaan berikutnya") }
        } else {
            val average = if (scores.isEmpty()) 0 else scores.average().toInt()
            Text("Hasil simulasi: $average / 100", style = MaterialTheme.typography.headlineSmall)
            Text("Skor ini hanya indikator kelengkapan jawaban, bukan prediksi kelulusan wawancara.")
            questions.forEachIndexed { i, question ->
                HorizontalDivider()
                Text("${i + 1}. ${question.question}", style = MaterialTheme.typography.titleSmall)
                Text("Jawaban: ${answers[i]}")
                Text("Skor: ${scores[i]}/100")
                Text("Saran: ${answerFeedback(answers[i], question).second}")
            }
            Text("Terus berlatih. Jawaban yang spesifik dan jujur akan lebih meyakinkan.")
            Button(onClick = {
                started = false
                finished = false
                answers = emptyList()
                index = 0
                answer = ""
            }) { Text("Latihan lagi / ganti posisi") }
        }
    }
}
