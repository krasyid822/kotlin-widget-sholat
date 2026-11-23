package com.widgetsholat.app

import com.widgetsholat.app.domain.model.CoordinatesInput
import com.widgetsholat.app.domain.model.DailyPrayerSchedule
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.util.AnalogPrayerDialMapper
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AnalogPrayerDialMapperTest {

    private val tz = TimeZone.of("Asia/Jakarta")
    private val date = LocalDate(2026, 11, 23)

    private fun instantAt(hour: Int, minute: Int, dayOffset: Int = 0) =
        LocalDateTime(2026, 11, 23 + dayOffset, hour, minute).toInstant(tz)

    private fun sampleSchedule() = DailyPrayerSchedule(
        date = date,
        coordinates = CoordinatesInput(-6.2088, 106.8456, "Jakarta"),
        subuh = instantAt(4, 15),
        syuruqStartTime = instantAt(5, 45),
        syuruqEndTime = instantAt(6, 1),
        dzuhur = instantAt(11, 45),
        ashar = instantAt(15, 5),
        maghrib = instantAt(17, 50),
        isya = instantAt(19, 2),
        nextDaySubuh = instantAt(4, 15, dayOffset = 1)
    )

    @Test
    fun `maps all 6 prayer markers with valid timeText and active window`() {
        val schedule = sampleSchedule()
        val now = instantAt(12, 30)

        val state = AnalogPrayerDialMapper.map(schedule, now, tz)

        assertEquals(6, state.markers.size)
        assertEquals("Jakarta", state.locationLabel)
        assertNotNull(state.activeArc)

        val dzuhur = state.markers.first { it.prayerName == PrayerName.DZUHUR }
        assertTrue(dzuhur.isActive)
        assertEquals("11:45", dzuhur.timeText)

        val syuruq = state.markers.first { it.prayerName == PrayerName.SYURUQ }
        assertTrue(syuruq.isSyuruq)
        assertEquals("05:45", syuruq.timeText)
    }

    @Test
    fun `collision avoidance separates overlapping AM and PM prayer labels`() {
        val schedule = sampleSchedule()
        val now = instantAt(12, 30)

        val state = AnalogPrayerDialMapper.map(schedule, now, tz)
        val syuruq = state.markers.first { it.prayerName == PrayerName.SYURUQ }
        val maghrib = state.markers.first { it.prayerName == PrayerName.MAGHRIB }

        // Syuruq (05:45) and Maghrib (17:50) are within 2.5 degrees on the 12-hour dial
        val rawDiff = angularDistance(syuruq.dialAngleDegrees, maghrib.dialAngleDegrees)
        assertTrue(rawDiff < 10f)

        // Resolved label angles must be separated by at least 20 degrees
        val labelDiff = angularDistance(syuruq.labelAngleDegrees, maghrib.labelAngleDegrees)
        assertTrue("Expected labelDiff >= 20f but was $labelDiff", labelDiff >= 20f)
    }

    @Test
    fun `midnight to subuh resolves active Isya window and syuruqArc is always visible`() {
        val schedule = sampleSchedule()
        val midnightNow = instantAt(0, 5)

        val state = AnalogPrayerDialMapper.map(schedule, midnightNow, tz)

        assertNotNull(state.activeArc)
        assertTrue(state.statusLabel.startsWith("Isya"))
        assertNotNull(state.syuruqArc)
        assertTrue(state.syuruqArc!!.sweepAngle >= 3.5f)
    }

    private fun angularDistance(a: Float, b: Float): Float {
        val diff = abs(a - b) % 360f
        return if (diff > 180f) 360f - diff else diff
    }
}
