package com.example.automatesmartbot.ui.navigation

/** The header and Android Back both pop this history, including Analyst/letter pages. */
object CvNavigation {
    const val HOME = "CV Builder"
    fun push(history: List<String>, destination: String): List<String> =
        if (history.lastOrNull() == destination) history else history + destination
    fun pop(history: List<String>): List<String> = if (history.size > 1) history.dropLast(1) else history
}
