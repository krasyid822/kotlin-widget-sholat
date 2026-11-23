package com.widgetsholat.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

enum class TrackingExecutionMode(val label: String) {
    SENDIRI("Sendiri"),
    JAMAAH("Jamaah")
}

enum class TrackingLocation(val label: String) {
    MASJID("Masjid"),
    NON_MASJID("Nggak di Masjid")
}

enum class TrackingCategory(val level: Int, val label: String) {
    JAMAAH_MASJID(1, "Level 1 • Jamaah di Masjid"),
    JAMAAH_NON_MASJID(2, "Level 2 • Jamaah Non-Masjid"),
    SENDIRI_MASJID(3, "Level 3 • Sendiri di Masjid"),
    SENDIRI_NON_MASJID(4, "Level 4 • Sendiri Non-Masjid"),
    QODHO(5, "Level 5 • Qodho")
}

enum class TrackingStatus {
    IN_PROGRESS,
    COMPLETED
}

data class PrayerTrackingSession(
    val id: Long = 0L,
    val date: LocalDate,
    val prayerName: PrayerName,
    val prayerStartTime: Instant,
    val prayerEndTime: Instant,
    val p0Timestamp: Instant? = null,
    val p1Mode: TrackingExecutionMode? = null,
    val p2Location: TrackingLocation? = null,
    val category: TrackingCategory? = null,
    val status: TrackingStatus = TrackingStatus.IN_PROGRESS
)
