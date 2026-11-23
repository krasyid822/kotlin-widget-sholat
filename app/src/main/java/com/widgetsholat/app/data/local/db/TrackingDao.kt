package com.widgetsholat.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {

    @Query("SELECT * FROM prayer_tracking_sessions WHERE status = 'COMPLETED' ORDER BY prayerStartTimeEpochMs DESC")
    fun observeCompleted(): Flow<List<TrackingEntity>>

    @Query("SELECT * FROM prayer_tracking_sessions WHERE status = 'IN_PROGRESS' ORDER BY prayerStartTimeEpochMs DESC LIMIT 1")
    suspend fun getLatestPending(): TrackingEntity?

    @Query("SELECT * FROM prayer_tracking_sessions WHERE dateIso = :dateIso AND prayerName = :prayerName LIMIT 1")
    suspend fun findByDateAndPrayer(dateIso: String, prayerName: String): TrackingEntity?

    @Query("SELECT * FROM prayer_tracking_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TrackingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrackingEntity): Long

    @Query("UPDATE prayer_tracking_sessions SET p0TimestampEpochMs = :p0Ms WHERE id = :id")
    suspend fun updateP0(id: Long, p0Ms: Long)

    @Query("UPDATE prayer_tracking_sessions SET p1Mode = :p1Mode WHERE id = :id")
    suspend fun updateP1(id: Long, p1Mode: String)

    @Query(
        "UPDATE prayer_tracking_sessions SET p2Location = :p2Location, category = :category, status = 'COMPLETED' WHERE id = :id"
    )
    suspend fun completeSession(id: Long, p2Location: String, category: String)
}
