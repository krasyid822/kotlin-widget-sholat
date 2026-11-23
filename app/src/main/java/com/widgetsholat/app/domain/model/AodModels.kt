package com.widgetsholat.app.domain.model

data class PrayerArcState(
    val startAngle: Float,
    val sweepAngle: Float,
    val isWarning: Boolean,
    val isSyuruq: Boolean,
    val label: String
)

data class AodClockState(
    val hourAngle: Float,
    val minuteAngle: Float,
    val shiftXDp: Float,
    val shiftYDp: Float,
    val timeText: String,
    val dayText: String = "",
    val batteryPercent: Int? = null,
    val arcState: PrayerArcState?,
    val syuruqArc: PrayerArcState? = null
)
