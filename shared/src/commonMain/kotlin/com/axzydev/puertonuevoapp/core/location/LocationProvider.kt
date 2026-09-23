package com.axzydev.puertonuevoapp.core.location

/** Ubicación puntual capturada para un escaneo (best-effort). */
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
)

/**
 * Proveedor de ubicación puntual. `currentLocation` debe resolver con un
 * timeout corto y devolver `null` ante cualquier fallo (permiso denegado,
 * GPS apagado, sin fix) — nunca lanza excepción al caller: el registro de
 * acceso continúa con `locationSource = SITE_ONLY`.
 */
interface LocationProvider {
    suspend fun currentLocation(): DeviceLocation?
}

/** Implementación de la plataforma (Android: LocationManager; iOS: stub Fase 4). */
expect fun platformLocationProvider(): LocationProvider
