package com.widgetsholat.app.domain.util

import com.widgetsholat.app.domain.model.TrackingCategory
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import kotlinx.datetime.Instant

object PrayerTrackingEvaluator {

    fun evaluate(
        p0Timestamp: Instant,
        prayerEndTime: Instant,
        p1Mode: TrackingExecutionMode,
        p2Location: TrackingLocation
    ): TrackingCategory {
        if (p0Timestamp > prayerEndTime) {
            return TrackingCategory.QODHO
        }
        return when (p1Mode) {
            TrackingExecutionMode.JAMAAH -> when (p2Location) {
                TrackingLocation.MASJID -> TrackingCategory.JAMAAH_MASJID
                TrackingLocation.NON_MASJID -> TrackingCategory.JAMAAH_NON_MASJID
            }
            TrackingExecutionMode.SENDIRI -> when (p2Location) {
                TrackingLocation.MASJID -> TrackingCategory.SENDIRI_MASJID
                TrackingLocation.NON_MASJID -> TrackingCategory.SENDIRI_NON_MASJID
            }
        }
    }
}
