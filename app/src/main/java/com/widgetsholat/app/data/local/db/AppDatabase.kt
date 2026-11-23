package com.widgetsholat.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackingDao(): TrackingDao
}
