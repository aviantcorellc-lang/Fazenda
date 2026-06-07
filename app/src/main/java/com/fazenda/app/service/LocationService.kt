package com.fazenda.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationService(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    suspend fun getCurrentLocation(timeoutMs: Long = 10_000): Location? {
        return try {
            withTimeout(timeoutMs) {
                getCurrentLocationInternal()
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getCurrentLocationInternal(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val lastKnownLocation = listOfNotNull(
            safeLastKnownLocation(LocationManager.GPS_PROVIDER),
            safeLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        ).maxByOrNull { it.time }

        if (lastKnownLocation != null) {
            continuation.resume(lastKnownLocation)
            return@suspendCancellableCoroutine
        }

        val provider = when {
            isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (continuation.isActive) {
                    continuation.resume(location)
                }
                locationManager.removeUpdates(this)
            }

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
                locationManager.removeUpdates(this)
            }
        }

        continuation.invokeOnCancellation {
            locationManager.removeUpdates(listener)
        }

        try {
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            continuation.resume(null)
        } catch (_: IllegalArgumentException) {
            continuation.resume(null)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun safeLastKnownLocation(provider: String): Location? {
        return try {
            locationManager.getLastKnownLocation(provider)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun isProviderEnabled(provider: String): Boolean {
        return try {
            locationManager.isProviderEnabled(provider)
        } catch (_: Exception) {
            false
        }
    }
}
