package com.example.automatesmartbot.data.psychotest

import org.junit.Assert.*
import org.junit.Test

class PsychotestPolicyTest {
    @Test fun questionNavigationStaysInBounds() {
        assertEquals(0, PsychotestPolicy.previous(0)); assertEquals(3, PsychotestPolicy.previous(4))
        assertEquals(9, PsychotestPolicy.next(9, 10)); assertEquals(1, PsychotestPolicy.next(0, 10))
    }
    @Test fun scoresUseWeightedMaximumAndRangeOneToHundred() {
        assertEquals(1, PsychotestPolicy.normalizedScore(0.0, 20.0))
        assertEquals(50, PsychotestPolicy.normalizedScore(25.0, 50.0))
        assertEquals(100, PsychotestPolicy.normalizedScore(50.0, 50.0))
        assertNull(PsychotestPolicy.normalizedScore(null, null))
        assertNull(PsychotestPolicy.normalizedScore(5.0, 0.0))
        assertNull(PsychotestPolicy.normalizedScore(Double.NaN, 20.0))
    }
}
