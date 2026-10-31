package com.example.automatesmartbot.data.career

import java.math.BigDecimal
import java.math.RoundingMode

/** Transparent annualized estimate; monthly TER payroll reconciliation is not inferred. */
object PayrollCalculator {
    enum class OvertimeDay { WORKDAY, REST_FIVE_DAY_WEEK, REST_SIX_DAY_WEEK, SHORT_DAY_REST }
    data class Input(
        val monthlyGross: Long, val overtimeBase: Long, val overtimeHours: Int,
        val overtimeDay: OvertimeDay, val bpjsHealthBase: Long,
        val jhtBase: Long, val pensionBase: Long,
        val ptkp: Long = 54_000_000, val serviceMonths: Int = 12,
        val thrBase: Long = monthlyGross
    )
    data class Result(val overtime: Long, val health: Long, val jht: Long, val pension: Long,
        val thr: Long, val annualTax: Long, val monthlyTaxEstimate: Long, val monthlyTakeHomeEstimate: Long)
    private fun money(value: BigDecimal) = value.setScale(0, RoundingMode.HALF_UP).longValueExact()
    fun overtime(base: Long, hours: Int, day: OvertimeDay): Long {
        require(base in 0..100_000_000_000 && hours in 0..12)
        require(day != OvertimeDay.WORKDAY || hours <= 4) { "Lembur hari kerja maksimal 4 jam/hari; batas mingguan perlu diperiksa terpisah." }
        val units = (1..hours).sumOf { hour -> when(day) {
            OvertimeDay.WORKDAY -> if (hour == 1) 1.5 else 2.0
            OvertimeDay.REST_FIVE_DAY_WEEK -> if (hour <= 8) 2.0 else if (hour == 9) 3.0 else 4.0
            OvertimeDay.REST_SIX_DAY_WEEK -> if (hour <= 7) 2.0 else if (hour == 8) 3.0 else 4.0
            OvertimeDay.SHORT_DAY_REST -> if (hour <= 5) 2.0 else if (hour == 6) 3.0 else 4.0
        } }
        return money(BigDecimal.valueOf(base).multiply(BigDecimal.valueOf(units)).divide(BigDecimal(173), 10, RoundingMode.HALF_UP))
    }
    fun annualIncomeTax(taxable: Long): Long {
        var remaining = (taxable.coerceAtLeast(0) / 1000) * 1000
        val widths = listOf(60_000_000L to "0.05", 190_000_000L to "0.15", 250_000_000L to "0.25", 4_500_000_000L to "0.30", Long.MAX_VALUE to "0.35")
        var tax = BigDecimal.ZERO
        for ((width, rate) in widths) {
            val amount = minOf(remaining, width)
            tax = tax.add(BigDecimal.valueOf(amount).multiply(BigDecimal(rate)))
            remaining -= amount
            if (remaining == 0L) break
        }
        return money(tax)
    }
    fun calculate(input: Input): Result {
        with(input) {
            require(listOf(monthlyGross, overtimeBase, bpjsHealthBase, jhtBase, pensionBase, thrBase).all { it in 0..100_000_000_000 })
            require(ptkp in 0..1_000_000_000 && serviceMonths in 0..1200)
            val overtime = overtime(overtimeBase, overtimeHours, overtimeDay)
            val health = money(BigDecimal.valueOf(minOf(bpjsHealthBase, 12_000_000)).multiply(BigDecimal("0.01")))
            val jht = money(BigDecimal.valueOf(jhtBase).multiply(BigDecimal("0.02")))
            val pension = money(BigDecimal.valueOf(pensionBase).multiply(BigDecimal("0.01")))
            val thr = if (serviceMonths < 1) 0 else money(BigDecimal.valueOf(thrBase).multiply(BigDecimal(minOf(12, serviceMonths))).divide(BigDecimal(12), 10, RoundingMode.HALF_UP))
            val annualGross = monthlyGross * 12 + thr + overtime
            val jobExpense = minOf(6_000_000L, money(BigDecimal.valueOf(annualGross).multiply(BigDecimal("0.05"))))
            val annualTax = annualIncomeTax((annualGross - jobExpense - (jht + pension) * 12 - ptkp).coerceAtLeast(0))
            val monthlyTax = money(BigDecimal.valueOf(annualTax).divide(BigDecimal(12), 10, RoundingMode.HALF_UP))
            return Result(overtime, health, jht, pension, thr, annualTax, monthlyTax, monthlyGross + overtime - health - jht - pension - monthlyTax)
        }
    }
}
