package com.example.disasternet.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _locationLabel = MutableStateFlow("Determining GPS coordinates...")
    val locationLabel: StateFlow<String> = _locationLabel.asStateFlow()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationCallback != null) return

        try {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _currentLocation.value = loc
                    resolveLocationLabel(loc.latitude, loc.longitude)
                }
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2500L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    _currentLocation.value = loc
                    resolveLocationLabel(loc.latitude, loc.longitude)
                }
            }

            fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            _locationLabel.value = "GPS Permission not granted"
        } catch (e: Exception) {
            _locationLabel.value = "GPS Location unavailable"
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun resolveLocationLabel(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: "Zone"
                            val feature = addr.featureName ?: ""
                            _locationLabel.value = if (feature.isNotBlank() && feature != thoroughfare) {
                                "$feature, $thoroughfare"
                            } else {
                                thoroughfare
                            }
                        } else {
                            _locationLabel.value = String.format(Locale.US, "GPS: %.4f, %.4f", lat, lon)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: addr.locality ?: "Zone"
                        val feature = addr.featureName ?: ""
                        _locationLabel.value = if (feature.isNotBlank() && feature != thoroughfare) {
                            "$feature, $thoroughfare"
                        } else {
                            thoroughfare
                        }
                    } else {
                        _locationLabel.value = String.format(Locale.US, "GPS: %.4f, %.4f", lat, lon)
                    }
                }
            } catch (e: Exception) {
                _locationLabel.value = String.format(Locale.US, "GPS: %.4f, %.4f", lat, lon)
            }
        }
    }

    companion object {
        const val ARRIVAL_DISTANCE_METERS = 120.0
        const val COMMUNITY_ALERT_RADIUS_METERS = 1500.0
        const val STALE_THRESHOLD_MILLIS = 120_000L // 2 minutes

        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0].toDouble()
        }

        fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val lat1Rad = Math.toRadians(lat1)
            val lon1Rad = Math.toRadians(lon1)
            val lat2Rad = Math.toRadians(lat2)
            val lon2Rad = Math.toRadians(lon2)

            val dLon = lon2Rad - lon1Rad
            val y = sin(dLon) * cos(lat2Rad)
            val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
            val bearingRad = atan2(y, x)
            var bearingDeg = Math.toDegrees(bearingRad).toFloat()
            if (bearingDeg < 0) bearingDeg += 360f
            return bearingDeg
        }

        fun bearingToCompass(bearing: Float): String {
            val normalized = (bearing % 360 + 360) % 360
            return when {
                normalized >= 337.5 || normalized < 22.5 -> "N"
                normalized < 67.5 -> "NE"
                normalized < 112.5 -> "E"
                normalized < 157.5 -> "SE"
                normalized < 202.5 -> "S"
                normalized < 247.5 -> "SW"
                normalized < 292.5 -> "W"
                else -> "NW"
            }
        }

        fun isLocationStale(timestampMillis: Long): Boolean {
            return (System.currentTimeMillis() - timestampMillis) > STALE_THRESHOLD_MILLIS
        }

        fun isWithinArrivalThreshold(distanceMeters: Double): Boolean {
            return distanceMeters <= ARRIVAL_DISTANCE_METERS
        }

        fun isWithinCommunityAlertRadius(distanceMeters: Double): Boolean {
            return distanceMeters <= COMMUNITY_ALERT_RADIUS_METERS
        }
    }
}
