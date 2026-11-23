package com.widgetsholat.app.domain.util

import com.widgetsholat.app.domain.model.AnalogPrayerMarker
import com.widgetsholat.app.domain.model.AnalogWidgetDialState
import com.widgetsholat.app.domain.model.DailyPrayerSchedule
import com.widgetsholat.app.domain.model.PrayerName
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Maps a [DailyPrayerSchedule] and current [Instant] into an [AnalogWidgetDialState]
 * with 12-hour dial angles and collision-avoided outer label angles.
 */
object AnalogPrayerDialMapper {

    private const val MIN_LABEL_SEPARATION_DEGREES = 24f

    fun map(
        schedule: DailyPrayerSchedule,
        now: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): AnalogWidgetDialState {
        val nowLocal = now.toLocalDateTime(timeZone)
        val hourAngle = PrayerArcCalculator.timeToDialAngle(nowLocal.hour, nowLocal.minute)
        val minuteAngle = nowLocal.minute * 6f - 90f
        val currentTimeText = "%02d:%02d".format(nowLocal.hour, nowLocal.minute)

        val locationLabel = schedule.coordinates.label.ifBlank {
            CoordinateParser.format(schedule.coordinates)
        }

        val activeWindow = schedule.resolveActiveWindow(now)
        val activeArc = activeWindow?.let {
            PrayerArcCalculator.calculateArc(now, it, timeZone)
        }
        val syuruqArc = PrayerArcCalculator.calculateSyuruqDurationArc(
            syuruqStartTime = schedule.syuruqStartTime,
            syuruqEndTime = schedule.syuruqEndTime,
            timeZone = timeZone
        )

        val statusLabel = activeArc?.label ?: run {
            val subuhLocal = schedule.subuh.toLocalDateTime(timeZone)
            "Menuju Subuh • %02d:%02d".format(subuhLocal.hour, subuhLocal.minute)
        }

        val rawMarkers = listOf(
            PrayerName.SUBUH to schedule.subuh,
            PrayerName.SYURUQ to schedule.syuruqStartTime,
            PrayerName.DZUHUR to schedule.dzuhur,
            PrayerName.ASHAR to schedule.ashar,
            PrayerName.MAGHRIB to schedule.maghrib,
            PrayerName.ISYA to schedule.isya
        ).map { (name, instant) ->
            val local = instant.toLocalDateTime(timeZone)
            val dialAngle = normalizeAngle(PrayerArcCalculator.timeToDialAngle(local.hour, local.minute))
            AnalogPrayerMarker(
                prayerName = name,
                timeText = "%02d:%02d".format(local.hour, local.minute),
                dialAngleDegrees = dialAngle,
                labelAngleDegrees = dialAngle,
                isActive = activeWindow?.prayerName == name,
                isSyuruq = name == PrayerName.SYURUQ
            )
        }

        val resolvedMarkers = resolveLabelCollisions(rawMarkers)

        return AnalogWidgetDialState(
            hourAngle = hourAngle,
            minuteAngle = minuteAngle,
            currentTimeText = currentTimeText,
            locationLabel = locationLabel,
            activeArc = activeArc,
            statusLabel = statusLabel,
            markers = resolvedMarkers,
            syuruqArc = syuruqArc
        )
    }

    private fun resolveLabelCollisions(markers: List<AnalogPrayerMarker>): List<AnalogPrayerMarker> {
        if (markers.size <= 1) return markers

        val indexed = markers.mapIndexed { index, marker -> index to marker.labelAngleDegrees }
            .sortedBy { it.second }
            .toMutableList()

        // Forward pass to push overlapping labels apart
        for (pass in 0 until 4) {
            for (i in indexed.indices) {
                val nextIdx = (i + 1) % indexed.size
                val currentAngle = indexed[i].second
                val nextAngle = indexed[nextIdx].second
                val diff = positiveAngularDiff(currentAngle, nextAngle)
                if (diff < MIN_LABEL_SEPARATION_DEGREES) {
                    val push = (MIN_LABEL_SEPARATION_DEGREES - diff) / 2f + 0.5f
                    indexed[i] = indexed[i].first to normalizeAngle(currentAngle - push)
                    indexed[nextIdx] = indexed[nextIdx].first to normalizeAngle(nextAngle + push)
                }
            }
        }

        val angleByIndex = indexed.toMap()
        return markers.mapIndexed { index, marker ->
            marker.copy(labelAngleDegrees = angleByIndex[index] ?: marker.labelAngleDegrees)
        }
    }

    private fun positiveAngularDiff(from: Float, to: Float): Float {
        var diff = (to - from) % 360f
        if (diff < 0f) diff += 360f
        return diff
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a < 0f) a += 360f
        return a
    }
}
