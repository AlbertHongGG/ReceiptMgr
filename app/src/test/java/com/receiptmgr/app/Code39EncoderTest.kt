package com.receiptmgr.app

import com.receiptmgr.app.domain.barcode.code39.Code39Encoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Code39EncoderTest {

    private val encoder = Code39Encoder.Default

    @Test
    fun `Taiwan carrier code 8J722KT encodes with correct element count and alternating bars`() {
        val pattern = encoder.encode("/8J722KT")

        assertEquals("/8J722KT", pattern.rawContent)

        // 10 characters (including * at both ends):
        // 10 * 9 elements + 9 inter-character gaps = 99 elements
        assertEquals(99, pattern.elements.size)

        // Verify strictly alternating bars and spaces
        for (i in pattern.elements.indices) {
            val expectedBar = (i % 2 == 0)
            assertEquals("Element at index $i should alternate", expectedBar, pattern.elements[i].isBar)
            assertTrue("Unit width must be 1 or 2", pattern.elements[i].unitWidth in 1..2)
        }

        // Verify start character '*' (elements 0 to 8: NWNNWNWNN)
        // 0: Bar(1), 1: Space(2), 2: Bar(1), 3: Space(1), 4: Bar(2), 5: Space(1), 6: Bar(2), 7: Space(1), 8: Bar(1)
        val startElements = pattern.elements.take(9)
        assertEquals(1, startElements[0].unitWidth) // N
        assertEquals(2, startElements[1].unitWidth) // W
        assertEquals(1, startElements[2].unitWidth) // N
        assertEquals(1, startElements[3].unitWidth) // N
        assertEquals(2, startElements[4].unitWidth) // W
        assertEquals(1, startElements[5].unitWidth) // N
        assertEquals(2, startElements[6].unitWidth) // W
        assertEquals(1, startElements[7].unitWidth) // N
        assertEquals(1, startElements[8].unitWidth) // N

        // Verify gap between * and / (element index 9 is space with width 1)
        assertEquals(false, pattern.elements[9].isBar)
        assertEquals(1, pattern.elements[9].unitWidth)

        // Verify quiet zones
        assertEquals(10, pattern.quietZoneUnits)
        assertTrue("Total units includes quiet zones", pattern.totalUnits > pattern.elements.sumOf { it.unitWidth })
    }

    @Test
    fun `lowercase carrier code normalizes to uppercase`() {
        val patternLower = encoder.encode("/8j722kt")
        val patternUpper = encoder.encode("/8J722KT")

        assertEquals(patternUpper.elements, patternLower.elements)
        assertEquals("/8J722KT", patternLower.rawContent)
    }

    @Test
    fun `validation detects valid and invalid characters`() {
        assertTrue(encoder.isValid("/8J722KT"))
        assertTrue(encoder.isValid("/ABC-123"))
        assertTrue(encoder.isValid("/+.- 123"))

        assertFalse(encoder.isValid(""))
        assertFalse(encoder.isValid("/8J722KT@")) // '@' is invalid
        assertFalse(encoder.isValid("/8J722KT!")) // '!' is invalid
        assertFalse(encoder.isValid("載具"))       // Chinese characters invalid
    }
}
