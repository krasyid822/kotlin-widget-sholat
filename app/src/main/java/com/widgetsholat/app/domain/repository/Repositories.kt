package com.widgetsholat.app.domain.repository

import com.widgetsholat.app.domain.model.CoordinatesInput
import com.widgetsholat.app.domain.model.DailyPrayerSchedule
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class AppSettings(
    val coordinates: CoordinatesInput? = null,
    val customAudioUri: String? = null,
    val aodEnabled: Boolean = false,
    val trackingEnabled: Boolean = false
)

interface PrayerRepository {
    fun getScheduleForDate(date: LocalDate, coordinates: CoordinatesInput): DailyPrayerSchedule
}

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun saveCoordinates(coordinates: CoordinatesInput)
    suspend fun saveCustomAudioUri(uri: String?)
    suspend fun setAodEnabled(enabled: Boolean)
    suspend fun setTrackingEnabled(enabled: Boolean)
}

interface TrackingRepository {
    fun observeAllCompleted(): Flow<List<PrayerTrackingSession>>
    suspend fun createOrGetSession(
        date: LocalDate,
        prayerName: PrayerName,
        prayerStartTime: Instant,
        prayerEndTime: Instant
    ): PrayerTrackingSession
    suspend fun getLatestPendingSession(): PrayerTrackingSession?
    suspend fun getSessionById(id: Long): PrayerTrackingSession?
    suspend fun recordP0(sessionId: Long, p0: Instant)
    suspend fun recordP1(sessionId: Long, p1: TrackingExecutionMode)
    suspend fun recordP2AndComplete(sessionId: Long, p2: TrackingLocation): PrayerTrackingSession?
}
