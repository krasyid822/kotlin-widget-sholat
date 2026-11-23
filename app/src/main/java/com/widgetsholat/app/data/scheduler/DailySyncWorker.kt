package com.widgetsholat.app.data.scheduler

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.presentation.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit

class DailySyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val prayerRepository: PrayerRepository,
    private val alarmScheduler: PrayerAlarmScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settingsFlow.first()
        val coords = settings.coordinates ?: return Result.success()
        val today = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val schedule = prayerRepository.getScheduleForDate(today, coords)
        alarmScheduler.scheduleDailyAlarms(schedule, settings.trackingEnabled)
        WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_prayer_sync"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySyncWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
