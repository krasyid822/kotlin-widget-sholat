package com.widgetsholat.app

import com.widgetsholat.app.domain.model.TrackingCategory
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import com.widgetsholat.app.domain.util.PrayerTrackingEvaluator
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTrackingEvaluatorTest {

    private val endTime = Instant.fromEpochSeconds(1_700_000_000L)
    private val onTimeP0 = Instant.fromEpochSeconds(1_699_999_000L)
    private val lateP0 = Instant.fromEpochSeconds(1_700_000_100L)

    @Test
    fun `evaluates all 5 tracking levels accurately`() {
        assertEquals(
            TrackingCategory.JAMAAH_MASJID,
            PrayerTrackingEvaluator.evaluate(onTimeP0, endTime, TrackingExecutionMode.JAMAAH, TrackingLocation.MASJID)
        )
        assertEquals(
            TrackingCategory.JAMAAH_NON_MASJID,
            PrayerTrackingEvaluator.evaluate(onTimeP0, endTime, TrackingExecutionMode.JAMAAH, TrackingLocation.NON_MASJID)
        )
        assertEquals(
            TrackingCategory.SENDIRI_MASJID,
            PrayerTrackingEvaluator.evaluate(onTimeP0, endTime, TrackingExecutionMode.SENDIRI, TrackingLocation.MASJID)
        )
        assertEquals(
            TrackingCategory.SENDIRI_NON_MASJID,
            PrayerTrackingEvaluator.evaluate(onTimeP0, endTime, TrackingExecutionMode.SENDIRI, TrackingLocation.NON_MASJID)
        )
        assertEquals(
            TrackingCategory.QODHO,
            PrayerTrackingEvaluator.evaluate(lateP0, endTime, TrackingExecutionMode.JAMAAH, TrackingLocation.MASJID)
        )
    }
}
