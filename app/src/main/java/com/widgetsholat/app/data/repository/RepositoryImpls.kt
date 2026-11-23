package com.widgetsholat.app.data.repository

import com.widgetsholat.app.data.adhan.AdhanPrayerCalculator
import com.widgetsholat.app.data.local.SettingsDataStore
import com.widgetsholat.app.data.local.db.TrackingDao
import com.widgetsholat.app.data.local.db.TrackingEntity
import com.widgetsholat.app.domain.model.CoordinatesInput
import com.widgetsholat.app.domain.model.DailyPrayerSchedule
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import com.widgetsholat.app.domain.model.TrackingStatus
import com.widgetsholat.app.domain.repository.AppSettings
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.repository.TrackingRepository
import com.widgetsholat.app.domain.util.PrayerTrackingEvaluator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class PrayerRepositoryImpl(
    private val calculator: AdhanPrayerCalculator
) : PrayerRepository {
    override fun getScheduleForDate(
        date: LocalDate,
        coordinates: CoordinatesInput
    ): DailyPrayerSchedule = calculator.calculateForDate(date, coordinates)
}

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun saveCoordinates(coordinates: CoordinatesInput) =
        dataStore.saveCoordinates(coordinates)

    override suspend fun saveCustomAudioUri(uri: String?) =
        dataStore.saveCustomAudioUri(uri)

    override suspend fun setAodEnabled(enabled: Boolean) =
        dataStore.setAodEnabled(enabled)

    override suspend fun setTrackingEnabled(enabled: Boolean) =
        dataStore.setTrackingEnabled(enabled)
}

class TrackingRepositoryImpl(
    private val dao: TrackingDao
) : TrackingRepository {

    override fun observeAllCompleted(): Flow<List<PrayerTrackingSession>> =
        dao.observeCompleted().map { list -> list.map { it.toDomain() } }

    override suspend fun createOrGetSession(
        date: LocalDate,
        prayerName: PrayerName,
        prayerStartTime: Instant,
        prayerEndTime: Instant
    ): PrayerTrackingSession {
        val existing = dao.findByDateAndPrayer(date.toString(), prayerName.name)
        if (existing != null) {
            return existing.toDomain()
        }
        val entity = TrackingEntity(
            dateIso = date.toString(),
            prayerName = prayerName.name,
            prayerStartTimeEpochMs = prayerStartTime.toEpochMilliseconds(),
            prayerEndTimeEpochMs = prayerEndTime.toEpochMilliseconds(),
            status = TrackingStatus.IN_PROGRESS.name
        )
        val id = dao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun getLatestPendingSession(): PrayerTrackingSession? =
        dao.getLatestPending()?.toDomain()

    override suspend fun getSessionById(id: Long): PrayerTrackingSession? =
        dao.getById(id)?.toDomain()

    override suspend fun recordP0(sessionId: Long, p0: Instant) {
        dao.updateP0(sessionId, p0.toEpochMilliseconds())
    }

    override suspend fun recordP1(sessionId: Long, p1: TrackingExecutionMode) {
        dao.updateP1(sessionId, p1.name)
    }

    override suspend fun recordP2AndComplete(
        sessionId: Long,
        p2: TrackingLocation
    ): PrayerTrackingSession? {
        val session = dao.getById(sessionId)?.toDomain() ?: return null
        val p0 = session.p0Timestamp ?: return null
        val p1 = session.p1Mode ?: return null
        val category = PrayerTrackingEvaluator.evaluate(
            p0Timestamp = p0,
            prayerEndTime = session.prayerEndTime,
            p1Mode = p1,
            p2Location = p2
        )
        dao.completeSession(sessionId, p2.name, category.name)
        return dao.getById(sessionId)?.toDomain()
    }
}
