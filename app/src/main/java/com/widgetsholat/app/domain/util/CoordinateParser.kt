package com.widgetsholat.app.domain.util

import com.widgetsholat.app.domain.model.CoordinatesInput
import java.util.Locale
import kotlin.math.abs

object CoordinateParser {

    fun parseSmartPaste(raw: String): Pair<Double, Double>? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split(",", ";", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        return if (isValid(lat, lon)) lat to lon else null
    }

    fun isValid(latitude: Double, longitude: Double): Boolean =
        latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !latitude.isNaN() &&
            !longitude.isNaN()

    fun format(coordinates: CoordinatesInput): String {
        val latDir = if (coordinates.latitude >= 0) "N" else "S"
        val lonDir = if (coordinates.longitude >= 0) "E" else "W"
        val latStr = "%.4f".format(Locale.US, abs(coordinates.latitude))
        val lonStr = "%.4f".format(Locale.US, abs(coordinates.longitude))
        return "$latStr° $latDir, $lonStr° $lonDir"
    }
}
