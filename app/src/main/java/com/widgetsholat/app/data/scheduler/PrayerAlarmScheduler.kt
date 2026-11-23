package com.widgetsholat.app.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.widgetsholat.app.domain.model.DailyPrayerSchedule
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.presentation.service.AlarmReceiver
import kotlinx.datetime.Clock

class PrayerAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleDailyAlarms(schedule: DailyPrayerSchedule, trackingEnabled: Boolean) {
        val nowMs = System.currentTimeMillis()

        PrayerName.entries.forEach { prayer ->
            val startMs = schedule.startTimeFor(prayer).toEpochMilliseconds()
            if (startMs > nowMs) {
                scheduleExact(
                    requestCode = prayer.ordinal + 100,
                    triggerAtMs = startMs,
                    action = AlarmReceiver.ACTION_PRAYER_ALERT,
                    prayerName = prayer.name,
                    endTimeMs = schedule.endTimeFor(prayer).toEpochMilliseconds()
                )
            }

            if (trackingEnabled && prayer.isObligatory) {
                val sesStartMs = startMs + 5 * 60_000L
                if (sesStartMs > nowMs) {
                    scheduleExact(
                        requestCode = prayer.ordinal + 200,
                        triggerAtMs = sesStartMs,
                        action = AlarmReceiver.ACTION_TRACKING_START,
                        prayerName = prayer.name,
                        startTimeMs = startMs,
                        endTimeMs = schedule.endTimeFor(prayer).toEpochMilliseconds()
                    )
                }
            }
        }

        val syuruqEndMs = schedule.syuruqEndTime.toEpochMilliseconds()
        if (syuruqEndMs > nowMs) {
            scheduleExact(
                requestCode = 300,
                triggerAtMs = syuruqEndMs,
                action = AlarmReceiver.ACTION_WIDGET_REFRESH,
                prayerName = PrayerName.SYURUQ.name
            )
        }
    }

    fun scheduleTrackingReschedule(sessionId: Long, delayMinutes: Long = 5L) {
        val triggerAtMs = System.currentTimeMillis() + delayMinutes * 60_000L
        scheduleExact(
            requestCode = 400 + (sessionId % 1000).toInt(),
            triggerAtMs = triggerAtMs,
            action = AlarmReceiver.ACTION_TRACKING_RESUME,
            sessionId = sessionId
        )
    }

    fun scheduleNextAnalogWidgetTick() {
        val manager = alarmManager ?: return
        val nextMinuteMs = ((System.currentTimeMillis() / 60_000L) + 1L) * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ANALOG_WIDGET_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ANALOG_TICK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
        if (canExact) {
            manager.setExact(AlarmManager.RTC, nextMinuteMs, pendingIntent)
        } else {
            manager.set(AlarmManager.RTC, nextMinuteMs, pendingIntent)
        }
    }

    fun cancelAnalogWidgetTick() {
        val manager = alarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ANALOG_WIDGET_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ANALOG_TICK,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        manager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleExact(
        requestCode: Int,
        triggerAtMs: Long,
        action: String,
        prayerName: String? = null,
        startTimeMs: Long = 0L,
        endTimeMs: Long = 0L,
        sessionId: Long = -1L
    ) {
        val manager = alarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            prayerName?.let { putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, it) }
            putExtra(AlarmReceiver.EXTRA_START_TIME_MS, startTimeMs)
            putExtra(AlarmReceiver.EXTRA_END_TIME_MS, endTimeMs)
            putExtra(AlarmReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
        if (canExact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    companion object {
        private const val REQUEST_CODE_ANALOG_TICK = 301
    }
}
