package com.widgetsholat.app

import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.model.TrackingCategory
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import com.widgetsholat.app.domain.model.TrackingStatus
import com.widgetsholat.app.domain.util.TrackingChartAggregator
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingChartAggregatorTest {

    private val today = LocalDate(2026, 11, 24)
    private val baseInstant = Instant.parse("2026-11-24T05:00:00Z")

    private fun makeSession(
        id: Long,
        date: LocalDate,
        p1Mode: TrackingExecutionMode?,
        p2Location: TrackingLocation?,
        category: TrackingCategory?,
        status: TrackingStatus = TrackingStatus.COMPLETED
    ) = PrayerTrackingSession(
        id = id,
        date = date,
        prayerName = PrayerName.DZUHUR,
        prayerStartTime = baseInstant,
        prayerEndTime = baseInstant,
        p0Timestamp = baseInstant,
        p1Mode = p1Mode,
        p2Location = p2Location,
        category = category,
        status = status
    )

    @Test
    fun `empty sessions returns zero totals and 7 empty daily points`() {
        val summary = TrackingChartAggregator.aggregate(emptyList(), today = today)

        assertEquals(0, summary.totalSessions)
        assertEquals(0, summary.onTimePercentage)
        assertEquals(0, summary.jamaahPercentage)
        assertEquals(5, summary.categoryDistribution.size)
        summary.categoryDistribution.forEach {
            assertEquals(0, it.count)
            assertEquals(0f, it.fraction, 0.0001f)
        }
        assertEquals(7, summary.dailyPoints.size)
        assertEquals(LocalDate(2026, 11, 18), summary.dailyPoints.first().date)
        assertEquals(today, summary.dailyPoints.last().date)
        summary.dailyPoints.forEach { assertEquals(0, it.totalCount) }
    }

    @Test
    fun `mixed sessions compute percentages distribution and daily buckets accurately`() {
        val yesterday = LocalDate(2026, 11, 23)
        val sessions = listOf(
            makeSession(1, today, TrackingExecutionMode.JAMAAH, TrackingLocation.MASJID, TrackingCategory.JAMAAH_MASJID),
            makeSession(2, today, TrackingExecutionMode.JAMAAH, TrackingLocation.NON_MASJID, TrackingCategory.JAMAAH_NON_MASJID),
            makeSession(3, yesterday, TrackingExecutionMode.SENDIRI, TrackingLocation.MASJID, TrackingCategory.SENDIRI_MASJID),
            makeSession(4, yesterday, TrackingExecutionMode.SENDIRI, TrackingLocation.NON_MASJID, TrackingCategory.QODHO),
            // Should be ignored because IN_PROGRESS
            makeSession(5, today, TrackingExecutionMode.JAMAAH, TrackingLocation.MASJID, TrackingCategory.JAMAAH_MASJID, TrackingStatus.IN_PROGRESS)
        )

        val summary = TrackingChartAggregator.aggregate(sessions, today = today)

        assertEquals(4, summary.totalSessions)
        assertEquals(75, summary.onTimePercentage)
        assertEquals(50, summary.jamaahPercentage)

        val distMap = summary.categoryDistribution.associateBy { it.category }
        assertEquals(1, distMap[TrackingCategory.JAMAAH_MASJID]?.count)
        assertEquals(1, distMap[TrackingCategory.JAMAAH_NON_MASJID]?.count)
        assertEquals(1, distMap[TrackingCategory.SENDIRI_MASJID]?.count)
        assertEquals(0, distMap[TrackingCategory.SENDIRI_NON_MASJID]?.count)
        assertEquals(1, distMap[TrackingCategory.QODHO]?.count)

        val todayPoint = summary.dailyPoints.last()
        assertEquals(today, todayPoint.date)
        assertEquals(2, todayPoint.totalCount)
        assertEquals(1, todayPoint.countsByCategory[TrackingCategory.JAMAAH_MASJID])
        assertEquals(1, todayPoint.countsByCategory[TrackingCategory.JAMAAH_NON_MASJID])

        val yesterdayPoint = summary.dailyPoints[summary.dailyPoints.size - 2]
        assertEquals(yesterday, yesterdayPoint.date)
        assertEquals(2, yesterdayPoint.totalCount)
        assertEquals(1, yesterdayPoint.countsByCategory[TrackingCategory.SENDIRI_MASJID])
        assertEquals(1, yesterdayPoint.countsByCategory[TrackingCategory.QODHO])
    }
}
