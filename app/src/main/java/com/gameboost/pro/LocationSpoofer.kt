package com.gameboost.pro

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationSpoofer(val context: Context) {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var spoofJob: Job? = null
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

    companion object {
        val LOCATION_PRESETS = mapOf(
            "Tokyo JP" to Pair(35.6762, 139.6503),
            "London UK" to Pair(51.5074, -0.1278),
            "New York US" to Pair(40.7128, -74.0060),
            "Dubai UAE" to Pair(25.2048, 55.2708),
            "Singapore" to Pair(1.3521, 103.8198),
            "Mumbai IN" to Pair(19.0760, 72.8777)
        )
    }

    fun start(lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon
        try {
            lm.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, false, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)

        spoofJob?.cancel()
        spoofJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val loc = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = currentLat
                    longitude = currentLon
                    accuracy = 1f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                }
                lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc)
                delay(500)
            }
        }
    }

    fun stop() {
        spoofJob?.cancel()
        spoofJob = null
        try {
            lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false)
            lm.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun spoofFixed(lat: Double, lon: Double) {
        start(lat, lon)
    }

    fun spoofRandom(realLat: Double, realLon: Double, radiusKm: Double) {
        val r = radiusKm / 111.0
        val a = Math.random() * 2 * Math.PI
        start(realLat + r * Math.cos(a), realLon + r * Math.sin(a))
    }

    fun spoofPreset(presetName: String) {
        LOCATION_PRESETS[presetName]?.let { (lat, lon) ->
            start(lat, lon)
        }
    }

    fun isActive(): Boolean = spoofJob?.isActive == true

    fun getCurrentLat(): Double = currentLat

    fun getCurrentLon(): Double = currentLon
}