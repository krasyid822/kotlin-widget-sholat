package com.widgetsholat.app

import com.widgetsholat.app.domain.model.CoordinatesInput
import com.widgetsholat.app.domain.util.CoordinateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateParserTest {

    @Test
    fun `smart paste parses google maps format`() {
        val parsed = CoordinateParser.parseSmartPaste("  -6.2088, 106.8456  ")
        assertEquals(-6.2088 to 106.8456, parsed)
    }

    @Test
    fun `smart paste rejects out of range coordinates`() {
        assertNull(CoordinateParser.parseSmartPaste("95.0, 106.8456"))
        assertFalse(CoordinateParser.isValid(91.0, 0.0))
        assertFalse(CoordinateParser.isValid(0.0, -181.0))
        assertTrue(CoordinateParser.isValid(-6.2088, 106.8456))
    }

    @Test
    fun `format outputs cardinal directions`() {
        val formatted = CoordinateParser.format(CoordinatesInput(-6.2088, 106.8456))
        assertEquals("6.2088° S, 106.8456° E", formatted)
    }
}
