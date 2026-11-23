package com.widgetsholat.app.domain.util

import com.widgetsholat.app.domain.model.ActivePrayerWindow
import com.widgetsholat.app.domain.model.PrayerArcState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object PrayerArcCalculator {

    fun timeToDialAngle(hour: Int, minute: Int): Float {
        val h12 = (hour % 12) + (minute / 60f)
        return h12 * 30f - 90f
    }

    fun calculateArc(
        now: Instant,
        window: ActivePrayerWindow,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): PrayerArcState {
        val nowLocal = now.toLocalDateTime(timeZone)
        val endLocal = window.endTime.toLocalDateTime(timeZone)

        val startAngle = timeToDialAngle(nowLocal.hour, nowLocal.minute)
        val remainingMinutes = ((window.endTime - now).inWholeMinutes).coerceAtLeast(0L)

        val sweepAngle = if (remainingMinutes >= 12 * 60L) {
            360f
        } else {
            val endAngle = timeToDialAngle(endLocal.hour, endLocal.minute)
            val rawSweep = endAngle - startAngle
            if (rawSweep < 0f) rawSweep + 360f else rawSweep
        }

        val hours = remainingMinutes / 60L
        val mins = remainingMinutes % 60L
        val endTimeStr = "%02d:%02d".format(endLocal.hour, endLocal.minute)
        val remainingStr = if (hours > 0) "${hours}j ${mins}m" else "${mins}m"

        val label = if (window.isSyuruqForbidden) {
            "Syuruq • Selesai $endTimeStr (-$remainingStr)"
        } else {
            "${window.prayerName.displayName} • Habis $endTimeStr (-$remainingStr)"
        }

        return PrayerArcState(
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            isWarning = window.isSyuruqForbidden || remainingMinutes < 30L,
            isSyuruq = window.isSyuruqForbidden,
            label = label
        )
    }

    fun calculateSyuruqDurationArc(
        syuruqStartTime: Instant,
        syuruqEndTime: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): PrayerArcState {
        val startLocal = syuruqStartTime.toLocalDateTime(timeZone)
        val endLocal = syuruqEndTime.toLocalDateTime(timeZone)

        val startAngle = timeToDialAngle(startLocal.hour, startLocal.minute)
        val endAngle = timeToDialAngle(endLocal.hour, endLocal.minute)
        val rawSweep = endAngle - startAngle
        val normalizedSweep = if (rawSweep < 0f) rawSweep + 360f else rawSweep

        return PrayerArcState(
            startAngle = startAngle,
            sweepAngle = normalizedSweep.coerceAtLeast(3.5f),
            isWarning = true,
            isSyuruq = true,
            label = "Syuruq"
        )
    }
}
