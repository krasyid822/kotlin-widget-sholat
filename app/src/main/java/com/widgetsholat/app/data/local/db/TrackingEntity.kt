package com.widgetsholat.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.model.TrackingCategory
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import com.widgetsholat.app.domain.model.TrackingStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "prayer_tracking_sessions")
data class TrackingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateIso: String,
    val prayerName: String,
    val prayerStartTimeEpochMs: Long,
    val prayerEndTimeEpochMs: Long,
    val p0TimestampEpochMs: Long? = null,
    val p1Mode: String? = null,
    val p2Location: String? = null,
    val category: String? = null,
    val status: String
) {
    fun toDomain(): PrayerTrackingSession = PrayerTrackingSession(
        id = id,
        date = LocalDate.parse(dateIso),
        prayerName = PrayerName.valueOf(prayerName),
        prayerStartTime = Instant.fromEpochMilliseconds(prayerStartTimeEpochMs),
        prayerEndTime = Instant.fromEpochMilliseconds(prayerEndTimeEpochMs),
        p0Timestamp = p0TimestampEpochMs?.let { Instant.fromEpochMilliseconds(it) },
        p1Mode = p1Mode?.let { TrackingExecutionMode.valueOf(it) },
        p2Location = p2Location?.let { TrackingLocation.valueOf(it) },
        category = category?.let { TrackingCategory.valueOf(it) },
        status = TrackingStatus.valueOf(status)
    )
}
