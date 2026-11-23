package com.widgetsholat.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

class OfflineLocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(): Pair<Double, Double>? {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var bestLocation: Location? = null

        for (provider in providers) {
            if (!runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)) {
                continue
            }
            val candidate = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            if (candidate != null && (bestLocation == null || candidate.time > bestLocation.time)) {
                bestLocation = candidate
            }
        }
        return bestLocation?.let { it.latitude to it.longitude }
    }
}
