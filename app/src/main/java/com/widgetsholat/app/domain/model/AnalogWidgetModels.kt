package com.widgetsholat.app.domain.model

/**
 * Represents a single prayer time marker attached to the 12-hour analog clock dial.
 */
data class AnalogPrayerMarker(
    val prayerName: PrayerName,
    val timeText: String,
    val dialAngleDegrees: Float,
    val labelAngleDegrees: Float,
    val isActive: Boolean,
    val isSyuruq: Boolean
)

/**
 * Immutable state for rendering the 12-hour analog clock prayer widget.
 */
data class AnalogWidgetDialState(
    val hourAngle: Float,
    val minuteAngle: Float,
    val currentTimeText: String,
    val locationLabel: String,
    val activeArc: PrayerArcState?,
    val statusLabel: String,
    val markers: List<AnalogPrayerMarker>,
    val syuruqArc: PrayerArcState? = null
)
