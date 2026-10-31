package com.example.automatesmartbot.data.career

/** Parse and validate before calculation; text values remain owned by the form. */
object PayrollForm {
    data class Validation(val input: PayrollCalculator.Input?, val errors: Map<String, String>)
    fun validate(fields: Map<String, String>, day: PayrollCalculator.OvertimeDay): Validation {
        val errors = linkedMapOf<String, String>()
        fun number(key: String, max: Long): Long {
            val raw = fields[key].orEmpty().trim()
            val n = raw.toLongOrNull()
            when { raw.isBlank() -> errors[key] = "Wajib diisi. Gunakan 0 jika tidak berlaku."
                n == null -> errors[key] = "Gunakan angka bulat tanpa titik atau koma."
                n !in 0..max -> errors[key] = "Isi angka 0–$max." }
            return n ?: 0L
        }
        val gross = number("gross", 100_000_000_000)
        val base = number("base", 100_000_000_000)
        val thr = if (fields.containsKey("thr")) number("thr", 100_000_000_000) else base
        val maxHours = when(day) { PayrollCalculator.OvertimeDay.WORKDAY -> 4L; PayrollCalculator.OvertimeDay.REST_FIVE_DAY_WEEK -> 12L; PayrollCalculator.OvertimeDay.REST_SIX_DAY_WEEK -> 11L; PayrollCalculator.OvertimeDay.SHORT_DAY_REST -> 9L }
        val hours = number("hours", maxHours)
        val health = number("health", 100_000_000_000)
        val jht = number("jht", 100_000_000_000)
        val pension = number("pension", 100_000_000_000)
        val ptkp = number("ptkp", 1_000_000_000)
        val months = number("months", 1200)
        return Validation(if (errors.isEmpty()) PayrollCalculator.Input(gross, base, hours.toInt(), day, health, jht, pension, ptkp, months.toInt(), thr) else null, errors)
    }
}
