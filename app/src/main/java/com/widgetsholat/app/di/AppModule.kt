package com.widgetsholat.app.di

import androidx.room.Room
import com.widgetsholat.app.data.adhan.AdhanPrayerCalculator
import com.widgetsholat.app.data.local.SettingsDataStore
import com.widgetsholat.app.data.local.db.AppDatabase
import com.widgetsholat.app.data.location.OfflineLocationProvider
import com.widgetsholat.app.data.repository.PrayerRepositoryImpl
import com.widgetsholat.app.data.repository.SettingsRepositoryImpl
import com.widgetsholat.app.data.repository.TrackingRepositoryImpl
import com.widgetsholat.app.data.scheduler.DailySyncWorker
import com.widgetsholat.app.data.scheduler.PrayerAlarmScheduler
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.repository.TrackingRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val appModule = module {
    single { AdhanPrayerCalculator() }
    single { SettingsDataStore(androidContext()) }
    single { OfflineLocationProvider(androidContext()) }
    single { PrayerAlarmScheduler(androidContext()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "widget_sholat_db"
        ).build()
    }
    single { get<AppDatabase>().trackingDao() }

    single<PrayerRepository> { PrayerRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<TrackingRepository> { TrackingRepositoryImpl(get()) }

    workerOf(::DailySyncWorker)
}
