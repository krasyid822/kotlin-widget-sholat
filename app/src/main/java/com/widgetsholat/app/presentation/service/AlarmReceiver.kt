package com.widgetsholat.app.presentation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.widgetsholat.app.data.scheduler.PrayerAlarmScheduler
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.repository.TrackingRepository
import com.widgetsholat.app.presentation.aod.AodMonitorService
import com.widgetsholat.app.presentation.overlay.PrayerTrackerOverlayService
import com.widgetsholat.app.presentation.widget.AnalogPrayerGlanceWidget
import com.widgetsholat.app.presentation.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.max

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val settingsRepository: SettingsRepository by inject()
    private val prayerRepository: PrayerRepository by inject()
    private val trackingRepository: TrackingRepository by inject()
    private val alarmScheduler: PrayerAlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_PRAYER_ALERT -> handlePrayerAlert(context, intent)
                    ACTION_WIDGET_REFRESH -> WidgetUpdater.updateAll(context)
                    ACTION_ANALOG_WIDGET_TICK -> handleAnalogWidgetTick(context)
                    ACTION_TRACKING_START -> handleTrackingStart(context, intent)
                    ACTION_TRACKING_RESUME -> handleTrackingResume(context, intent)
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_DATE_CHANGED -> rescheduleToday(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handlePrayerAlert(context: Context, intent: Intent) {
        val prayerRaw = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val endTimeMs = intent.getLongExtra(EXTRA_END_TIME_MS, 0L)
        val nowMs = System.currentTimeMillis()
        val remainingHours = max(1L, (endTimeMs - nowMs) / 3_600_000L)

        val serviceIntent = Intent(context, PrayerAlertService::class.java).apply {
            putExtra(PrayerAlertService.EXTRA_PRAYER_NAME, prayerRaw)
            putExtra(PrayerAlertService.EXTRA_REMAINING_HOURS, remainingHours)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
        WidgetUpdater.updateAll(context)
    }

    private suspend fun handleTrackingStart(context: Context, intent: Intent) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.trackingEnabled) return

        val prayerRaw = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return
        val prayer = runCatching { PrayerName.valueOf(prayerRaw) }.getOrNull() ?: return
        if (!prayer.isObligatory) return

        val startMs = intent.getLongExtra(EXTRA_START_TIME_MS, 0L)
        val endMs = intent.getLongExtra(EXTRA_END_TIME_MS, 0L)
        val today = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        val session = trackingRepository.createOrGetSession(
            date = today,
            prayerName = prayer,
            prayerStartTime = Instant.fromEpochMilliseconds(startMs),
            prayerEndTime = Instant.fromEpochMilliseconds(endMs)
        )
        launchOverlay(context, session.id)
    }

    private suspend fun handleTrackingResume(context: Context, intent: Intent) {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.trackingEnabled) return

        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        val targetId = if (sessionId > 0L) {
            sessionId
        } else {
            trackingRepository.getLatestPendingSession()?.id ?: return
        }
        launchOverlay(context, targetId)
    }

    private fun launchOverlay(context: Context, sessionId: Long) {
        val overlayIntent = Intent(context, PrayerTrackerOverlayService::class.java).apply {
            putExtra(PrayerTrackerOverlayService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(context, overlayIntent)
    }

    private suspend fun rescheduleToday(context: Context) {
        val settings = settingsRepository.settingsFlow.first()
        if (settings.aodEnabled) {
            AodMonitorService.start(context)
        }
        val coords = settings.coordinates ?: return
        val today = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val schedule = prayerRepository.getScheduleForDate(today, coords)
        alarmScheduler.scheduleDailyAlarms(schedule, settings.trackingEnabled)
        if (GlanceAppWidgetManager(context).getGlanceIds(AnalogPrayerGlanceWidget::class.java).isNotEmpty()) {
            alarmScheduler.scheduleNextAnalogWidgetTick()
        }
        WidgetUpdater.updateAll(context)
    }

    private suspend fun handleAnalogWidgetTick(context: Context) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(AnalogPrayerGlanceWidget::class.java)
        if (ids.isNotEmpty()) {
            AnalogPrayerGlanceWidget.updateAllWithTick(context)
            alarmScheduler.scheduleNextAnalogWidgetTick()
        } else {
            alarmScheduler.cancelAnalogWidgetTick()
        }
    }

    companion object {
        const val ACTION_PRAYER_ALERT = "com.widgetsholat.app.ACTION_PRAYER_ALERT"
        const val ACTION_WIDGET_REFRESH = "com.widgetsholat.app.ACTION_WIDGET_REFRESH"
        const val ACTION_ANALOG_WIDGET_TICK = "com.widgetsholat.app.ACTION_ANALOG_WIDGET_TICK"
        const val ACTION_TRACKING_START = "com.widgetsholat.app.ACTION_TRACKING_START"
        const val ACTION_TRACKING_RESUME = "com.widgetsholat.app.ACTION_TRACKING_RESUME"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_START_TIME_MS = "extra_start_time_ms"
        const val EXTRA_END_TIME_MS = "extra_end_time_ms"
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
