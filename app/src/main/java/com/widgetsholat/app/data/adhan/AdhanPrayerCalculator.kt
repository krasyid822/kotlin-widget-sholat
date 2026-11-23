package com.widgetsholat.app.data.adhan

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.widgetsholat.app.domain.model.CoordinatesInput
import com.widgetsholat.app.domain.model.DailyPrayerSchedule
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class AdhanPrayerCalculator {

    fun calculateForDate(
        date: LocalDate,
        coordinatesInput: CoordinatesInput
    ): DailyPrayerSchedule {
        val coordinates = Coordinates(coordinatesInput.latitude, coordinatesInput.longitude)
        val dateComponents = DateComponents(date.year, date.monthNumber, date.dayOfMonth)

        val kemenagParams = CalculationParameters(
            fajrAngle = 20.0,
            ishaAngle = 18.0,
            madhab = Madhab.SHAFI,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
            method = CalculationMethod.OTHER
        )

        val prayerTimes = PrayerTimes(coordinates, dateComponents, kemenagParams)

        // Parameter Kemenag RI 7 hasta (+3.5° di atas ufuk = -3.5° pada adhan2)
        val syuruqEndParams = CalculationParameters(
            fajrAngle = -3.5,
            ishaAngle = 18.0,
            madhab = Madhab.SHAFI,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
            method = CalculationMethod.OTHER
        )
        val syuruqEndPrayerTimes = PrayerTimes(coordinates, dateComponents, syuruqEndParams)

        val nextDate = date.plus(1, DateTimeUnit.DAY)
        val nextDateComponents = DateComponents(
            nextDate.year,
            nextDate.monthNumber,
            nextDate.dayOfMonth
        )
        val nextDayPrayerTimes = PrayerTimes(coordinates, nextDateComponents, kemenagParams)

        val previousDate = date.minus(1, DateTimeUnit.DAY)
        val prevDateComponents = DateComponents(
            previousDate.year,
            previousDate.monthNumber,
            previousDate.dayOfMonth
        )
        val previousDayPrayerTimes = PrayerTimes(coordinates, prevDateComponents, kemenagParams)

        return DailyPrayerSchedule(
            date = date,
            coordinates = coordinatesInput,
            subuh = prayerTimes.fajr,
            syuruqStartTime = prayerTimes.sunrise,
            syuruqEndTime = syuruqEndPrayerTimes.fajr,
            dzuhur = prayerTimes.dhuhr,
            ashar = prayerTimes.asr,
            maghrib = prayerTimes.maghrib,
            isya = prayerTimes.isha,
            nextDaySubuh = nextDayPrayerTimes.fajr,
            previousDayIsya = previousDayPrayerTimes.isha
        )
    }
}
