package nz.beeaudit.app

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat

class GpsTracker(
    context: Context,
    private val onFix: (GpsFix) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onFix(location.toGpsFix())
        }

        override fun onProviderDisabled(provider: String) {
            onStatus("GPS provider disabled")
        }

        override fun onProviderEnabled(provider: String) {
            onStatus("Waiting for GPS fix")
        }

        @Deprecated("Deprecated by Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        if (!hasLocationPermission()) {
            onStatus("Location permission needed")
            return
        }
        val provider = bestProvider()
        if (provider == null) {
            onStatus("No location provider")
            return
        }
        runCatching {
            locationManager.getLastKnownLocation(provider)?.let { onFix(it.toGpsFix()) }
            locationManager.requestLocationUpdates(provider, 2_000L, 0f, listener)
            onStatus("Waiting for GPS fix")
        }.recoverCatching { error ->
            if (error is SecurityException) {
                onStatus("Location permission revoked")
            } else {
                throw error
            }
        }.onFailure {
            onStatus("GPS unavailable: ${it.message.orEmpty()}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (hasLocationPermission()) {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }

    private fun bestProvider(): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

private fun Location.toGpsFix(): GpsFix {
    return GpsFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        capturedAtMillis = time,
    )
}
