package com.example.automatesmartbot.data.career

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PayrollAndLettersTest {
    @Test fun overtimeUses173AndDaySpecificMultipliers() {
        val base=1_730_000L
        assertEquals(35_000L,PayrollCalculator.overtime(base,2,PayrollCalculator.OvertimeDay.WORKDAY))
        assertEquals(190_000L,PayrollCalculator.overtime(base,9,PayrollCalculator.OvertimeDay.REST_FIVE_DAY_WEEK))
        assertEquals(170_000L,PayrollCalculator.overtime(base,8,PayrollCalculator.OvertimeDay.REST_SIX_DAY_WEEK))
        assertEquals(130_000L,PayrollCalculator.overtime(base,6,PayrollCalculator.OvertimeDay.SHORT_DAY_REST))
        assertTrue(runCatching{PayrollCalculator.overtime(base,5,PayrollCalculator.OvertimeDay.WORKDAY)}.isFailure)
    }
    @Test fun annualTaxUsesHppBracketsAndTaxableThousandRounding() {
        assertEquals(0L,PayrollCalculator.annualIncomeTax(999))
        assertEquals(3_000_000L,PayrollCalculator.annualIncomeTax(60_000_000))
        assertEquals(31_500_000L,PayrollCalculator.annualIncomeTax(250_000_000))
        assertEquals(94_000_000L,PayrollCalculator.annualIncomeTax(500_000_000))
        assertEquals(1_444_000_000L,PayrollCalculator.annualIncomeTax(5_000_000_000))
    }
    @Test fun payrollThrAndWorkerContributionsAreCalculated() {
        val input=PayrollCalculator.Input(10_000_000,10_000_000,0,PayrollCalculator.OvertimeDay.WORKDAY,10_000_000,10_000_000,10_000_000)
        val r=PayrollCalculator.calculate(input)
        assertEquals(100_000L,r.health);assertEquals(200_000L,r.jht);assertEquals(100_000L,r.pension)
        assertEquals(10_000_000L,r.thr);assertEquals(3_960_000L,r.annualTax);assertEquals(9_270_000L,r.monthlyTakeHomeEstimate)
        assertEquals(5_000_000L,PayrollCalculator.calculate(input.copy(serviceMonths=6)).thr)
        assertEquals(0L,PayrollCalculator.calculate(input.copy(serviceMonths=0)).thr)
        assertEquals(120_000L,PayrollCalculator.calculate(input.copy(bpjsHealthBase=30_000_000)).health)
        assertTrue(runCatching{PayrollCalculator.calculate(input.copy(monthlyGross=-1))}.isFailure)
    }
    @Test fun resignationIncludesUserFactsAndValidatesDates() {
        val text=CareerLetters.resignation("Dina","PT Saat Ini","Admin",LocalDate.of(2026,10,31),LocalDate.of(2026,12,1),"Pendidikan lanjutan")
        assertTrue(text.contains("Dina"));assertTrue(text.contains("PT Saat Ini"));assertTrue(text.contains("2026-12-01"));assertTrue(text.contains("Pendidikan lanjutan"))
        assertTrue(runCatching{CareerLetters.resignation("Dina","PT","Admin",LocalDate.of(2026,12,1),LocalDate.of(2026,10,31),"")}.isFailure)
    }
    @Test fun trainingScoreRequiresAnswersAndUsesActualKeys() {
        TrainingCourses.all.forEach{course->
            assertEquals(100,TrainingCourses.score(course.questions.map{it.correct},course.questions))
            assertEquals(0,TrainingCourses.score(course.questions.map{null},course.questions))
            assertTrue(course.lessons.isNotEmpty())
        }
    }
}
