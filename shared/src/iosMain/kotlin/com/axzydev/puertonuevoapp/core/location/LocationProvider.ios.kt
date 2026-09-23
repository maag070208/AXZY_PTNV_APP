package com.axzydev.puertonuevoapp.core.location

/**
 * Stub de iOS para la Fase 1 (MVP Android-first). El GPS del guardia en iOS
 * se implementa en la Fase 4; por ahora devuelve `null` para que el registro
 * continúe con `locationSource = SITE_ONLY`.
 */
actual fun platformLocationProvider(): LocationProvider = IosLocationProvider()

private class IosLocationProvider : LocationProvider {
    override suspend fun currentLocation(): DeviceLocation? = null
}
