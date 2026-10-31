package com.example.automatesmartbot.data.psychotest

import kotlin.math.roundToInt

object PsychotestPolicy {
    fun normalizedScore(score: Double?, maximum: Double?): Int? {
        if (score == null || maximum == null || !score.isFinite() || !maximum.isFinite() || maximum <= 0) return null
        return (score / maximum * 100).roundToInt().coerceIn(1, 100)
    }
    fun previous(index: Int) = (index - 1).coerceAtLeast(0)
    fun next(index: Int, count: Int) = (index + 1).coerceAtMost((count - 1).coerceAtLeast(0))
}
