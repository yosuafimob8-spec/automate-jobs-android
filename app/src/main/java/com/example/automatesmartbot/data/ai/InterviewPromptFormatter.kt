package com.example.automatesmartbot.data.ai

/** Qwen chat prompt builder. Caller must provide bounded, trusted conversation history. */
object InterviewPromptFormatter {
    data class Turn(val role: String, val content: String)

    fun qwenInterviewPrompt(
        jobRole: String,
        history: List<Turn>,
        language: String = "Indonesia"
    ): String = buildString {
        val system = "Kamu HRD profesional yang mewawancarai kandidat untuk posisi " +
            jobRole.take(120) + ". Gunakan bahasa " + language.take(40) +
            ". Ajukan satu pertanyaan singkat tiap giliran. Sesuaikan pertanyaan dengan jawaban terakhir. " +
            "Jangan mengarang pengalaman kandidat."
        append("<|im_start|>system\n").append(system).append("<|im_end|>\n")
        history.takeLast(12).forEach { turn ->
            val role = if (turn.role == "assistant") "assistant" else "user"
            append("<|im_start|>").append(role).append('\n')
            append(turn.content.take(1500).replace("<|im_start|>", "").replace("<|im_end|>", ""))
            append("<|im_end|>\n")
        }
        append("<|im_start|>assistant\n")
    }
}
