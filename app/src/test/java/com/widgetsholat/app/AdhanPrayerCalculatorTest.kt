package com.widgetsholat.app

import com.widgetsholat.app.data.adhan.AdhanPrayerCalculator
import com.widgetsholat.app.domain.model.CoordinatesInput
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanPrayerCalculatorTest {

    private val calculator = AdhanPrayerCalculator()

    @Test
    fun `kemenag schedule orders prayers and computes positive syuruq 7 hasta duration`() {
        val jakarta = CoordinatesInput(latitude = -6.2088, longitude = 106.8456, label = "Jakarta")
        val date = LocalDate(2026, 5, 10)

        val schedule = calculator.calculateForDate(date, jakarta)

        assertTrue(schedule.subuh < schedule.syuruqStartTime)
        assertTrue(schedule.syuruqStartTime < schedule.syuruqEndTime)
        assertTrue(schedule.syuruqEndTime < schedule.dzuhur)
        assertTrue(schedule.dzuhur < schedule.ashar)
        assertTrue(schedule.ashar < schedule.maghrib)
        assertTrue(schedule.maghrib < schedule.isya)
        assertTrue(schedule.isya < schedule.nextDaySubuh)

        val syuruqMinutes = schedule.syuruqDuration.inWholeMinutes
        assertTrue("Expected syuruq duration > 0, got $syuruqMinutes", syuruqMinutes in 10..25)
    }
}
