package com.widgetsholat.app

import com.widgetsholat.app.domain.model.ActivePrayerWindow
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.util.PrayerArcCalculator
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerArcCalculatorTest {

    @Test
    fun `timeToDialAngle maps 12 oclock to -90 and 3 oclock to 0`() {
        assertEquals(-90f, PrayerArcCalculator.timeToDialAngle(12, 0), 0.01f)
        assertEquals(0f, PrayerArcCalculator.timeToDialAngle(3, 0), 0.01f)
        assertEquals(90f, PrayerArcCalculator.timeToDialAngle(18, 0), 0.01f)
    }

    @Test
    fun `calculateArc flags warning when remaining time under 30 minutes`() {
        val tz = TimeZone.UTC
        val now = LocalDateTime(2026, 5, 10, 14, 45).toInstant(tz)
        val end = LocalDateTime(2026, 5, 10, 15, 5).toInstant(tz)
        val window = ActivePrayerWindow(
            prayerName = PrayerName.DZUHUR,
            startTime = LocalDateTime(2026, 5, 10, 12, 0).toInstant(tz),
            endTime = end,
            isSyuruqForbidden = false
        )

        val arc = PrayerArcCalculator.calculateArc(now, window, tz)
        assertTrue(arc.isWarning)
        assertEquals(10f, arc.sweepAngle, 0.5f)
    }
}
