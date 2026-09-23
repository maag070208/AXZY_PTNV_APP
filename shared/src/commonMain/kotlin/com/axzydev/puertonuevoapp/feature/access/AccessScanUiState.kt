package com.axzydev.puertonuevoapp.feature.access

import com.axzydev.puertonuevoapp.core.network.access.AccessEventDto
import com.axzydev.puertonuevoapp.core.network.access.AccessLookupResultDto
import com.axzydev.puertonuevoapp.core.network.access.SiteDto

/** Fases del flujo de escaneo. */
enum class ScanPhase {
    /** Cámara lista esperando un QR (o error de escaneo pendiente de mostrar). */
    SCANNING,

    /** QR leído; resolviendo el empleado con `POST /access/lookup`. */
    LOOKING_UP,

    /** Empleado resuelto; el guardia confirma ENTRY/EXIT. */
    REVIEW,

    /** Evento registrado con éxito. */
    SUCCESS,
}

data class AccessScanUiState(
    val loadingSites: Boolean = true,
    val sitesError: String? = null,
    val sites: List<SiteDto> = emptyList(),
    val selectedSiteId: String? = null,

    val phase: ScanPhase = ScanPhase.SCANNING,

    /** Error de lectura/resolución del QR (no bloquea seguir escaneando). */
    val scanError: String? = null,

    val employee: AccessLookupResultDto? = null,

    /** Tipo que se registrará: sugerido por el backend, ajustable por el guardia. */
    val selectedType: String = "ENTRY",

    val submitting: Boolean = false,

    /** Error de `POST /access/events` (409 de negocio, red, etc.). */
    val errorMessage: String? = null,

    /** Evento confirmado por el servidor tras un alta exitosa. */
    val result: AccessEventDto? = null,

    /** `GPS` o `SITE_ONLY`, según si se obtuvo una ubicación al registrar. */
    val locationSource: String? = null,

    /**
     * Idempotencia del escaneo en curso. Se genera UNA vez por QR leído y se
     * reutiliza en cada reintento del mismo escaneo.
     */
    val pendingClientEventId: String? = null,

    /** QR crudo del escaneo en curso (viaja tal cual al API). */
    val rawQr: String? = null,
) {
    val selectedSite: SiteDto? get() = sites.firstOrNull { it.id == selectedSiteId }
    val hasSiteSelected: Boolean get() = !selectedSiteId.isNullOrBlank()
    val hasSites: Boolean get() = sites.isNotEmpty()
}
