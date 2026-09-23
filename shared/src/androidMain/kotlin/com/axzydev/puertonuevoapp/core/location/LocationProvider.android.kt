package com.axzydev.puertonuevoapp.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.axzydev.puertonuevoapp.core.session.AndroidAppContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

actual fun platformLocationProvider(): LocationProvider = AndroidLocationProvider()

private const val LOCATION_TIMEOUT_MS = 4_000L
private const val STALE_LAST_KNOWN_MS = 120_000L

/**
 * Ubicación puntual con el `LocationManager` del sistema (sin Google Play
 * Services). Prefiere un fix nuevo con timeout corto; si no llega, cae al
 * último punto conocido. Cualquier fallo se traduce a `null`.
 */
private class AndroidLocationProvider : LocationProvider {

    override suspend fun currentLocation(): DeviceLocation? = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
        try {
            val context = AndroidAppContext.appContext
            if (!hasLocationPermission(context)) return@withTimeoutOrNull null
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withTimeoutOrNull null
            val provider = bestProvider(manager) ?: return@withTimeoutOrNull null
            val fresh = requestSingle(manager, provider)
            (fresh ?: lastKnown(manager, provider))?.toDeviceLocation()
        } catch (e: Exception) {
            null
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun bestProvider(manager: LocationManager): String? = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }

    private fun lastKnown(manager: LocationManager, provider: String): Location? {
        val candidates = manager.allProviders.mapNotNull { candidate ->
            runCatching { manager.getLastKnownLocation(candidate) }.getOrNull()
        }
        val best = candidates.maxByOrNull { it.time } ?: return null
        return if (System.currentTimeMillis() - best.time <= STALE_LAST_KNOWN_MS) best else null
    }

    @Suppress("DEPRECATION")
    private suspend fun requestSingle(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {}

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            try {
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
}

private fun Location.toDeviceLocation(): DeviceLocation = DeviceLocation(
    latitude = latitude,
    longitude = longitude,
    accuracy = if (hasAccuracy()) accuracy.toDouble() else null,
)
