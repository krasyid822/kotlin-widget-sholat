package com.widgetsholat.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

enum class PrayerName(val displayName: String, val isObligatory: Boolean) {
    SUBUH("Subuh", true),
    SYURUQ("Syuruq", false),
    DZUHUR("Dzuhur", true),
    ASHAR("Ashar", true),
    MAGHRIB("Maghrib", true),
    ISYA("Isya", true)
}

data class CoordinatesInput(
    val latitude: Double,
    val longitude: Double,
    val label: String = ""
)

data class DailyPrayerSchedule(
    val date: LocalDate,
    val coordinates: CoordinatesInput,
    val subuh: Instant,
    val syuruqStartTime: Instant,
    val syuruqEndTime: Instant,
    val dzuhur: Instant,
    val ashar: Instant,
    val maghrib: Instant,
    val isya: Instant,
    val nextDaySubuh: Instant,
    val previousDayIsya: Instant? = null
) {
    val syuruqDuration: Duration
        get() = syuruqEndTime - syuruqStartTime

    fun isInSyuruqInterval(now: Instant): Boolean =
        now >= syuruqStartTime && now < syuruqEndTime

    fun endTimeFor(prayer: PrayerName): Instant = when (prayer) {
        PrayerName.SUBUH -> syuruqStartTime
        PrayerName.SYURUQ -> syuruqEndTime
        PrayerName.DZUHUR -> ashar
        PrayerName.ASHAR -> maghrib
        PrayerName.MAGHRIB -> isya
        PrayerName.ISYA -> nextDaySubuh
    }

    fun startTimeFor(prayer: PrayerName): Instant = when (prayer) {
        PrayerName.SUBUH -> subuh
        PrayerName.SYURUQ -> syuruqStartTime
        PrayerName.DZUHUR -> dzuhur
        PrayerName.ASHAR -> ashar
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISYA -> isya
    }

    fun resolveActiveWindow(now: Instant): ActivePrayerWindow? {
        if (now < subuh) {
            return ActivePrayerWindow(
                prayerName = PrayerName.ISYA,
                startTime = previousDayIsya ?: (subuh - 10.hours),
                endTime = subuh,
                isSyuruqForbidden = false
            )
        }
        val ordered = listOf(
            PrayerName.SUBUH to syuruqStartTime,
            PrayerName.SYURUQ to syuruqEndTime,
            PrayerName.DZUHUR to ashar,
            PrayerName.ASHAR to maghrib,
            PrayerName.MAGHRIB to isya,
            PrayerName.ISYA to nextDaySubuh
        )
        for ((name, end) in ordered) {
            val start = startTimeFor(name)
            if (now >= start && now < end) {
                return ActivePrayerWindow(
                    prayerName = name,
                    startTime = start,
                    endTime = end,
                    isSyuruqForbidden = name == PrayerName.SYURUQ
                )
            }
        }
        return null
    }
}

data class ActivePrayerWindow(
    val prayerName: PrayerName,
    val startTime: Instant,
    val endTime: Instant,
    val isSyuruqForbidden: Boolean
)
