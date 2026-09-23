package com.axzydev.puertonuevoapp.feature.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.location.LocationProvider
import com.axzydev.puertonuevoapp.core.network.access.AccessApi
import com.axzydev.puertonuevoapp.core.network.access.AccessEventInput
import com.axzydev.puertonuevoapp.core.network.access.AccessEventType
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.util.currentTimeMillis
import com.axzydev.puertonuevoapp.core.util.formatIsoUtc
import com.axzydev.puertonuevoapp.core.util.newClientEventId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orquesta el escaneo de una credencial: elegir sitio → leer QR → `lookup`
 * (mostrar nombre/foto/sugerencia) → capturar GPS → `POST /access/events`.
 *
 * El `clientEventId` se genera una sola vez por QR leído y se reutiliza en
 * los reintentos del mismo escaneo: es la garantía de idempotencia del
 * backend. Un escaneo nuevo (otro QR) genera un id nuevo.
 */
class AccessScanViewModel(
    private val accessApi: AccessApi,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessScanUiState())
    val uiState: StateFlow<AccessScanUiState> = _uiState.asStateFlow()

    init {
        loadSites()
    }

    fun loadSites() {
        _uiState.update { it.copy(loadingSites = true, sitesError = null) }
        viewModelScope.launch {
            val result = runCatching { accessApi.sites() }
            _uiState.update {
                it.copy(
                    loadingSites = false,
                    sitesError = result.exceptionOrNull()?.let(::networkMessage),
                    sites = result.getOrDefault(it.sites),
                )
            }
        }
    }

    fun selectSite(siteId: String) = _uiState.update { it.copy(selectedSiteId = siteId) }

    /** Lee un QR. Se ignora si ya hay un escaneo en curso. */
    fun onQrScanned(raw: String) {
        val state = _uiState.value
        if (state.phase == ScanPhase.LOOKING_UP || state.phase == ScanPhase.REVIEW || state.submitting) return
        if (!state.hasSiteSelected) {
            _uiState.update { it.copy(scanError = "Selecciona una portería antes de escanear") }
            return
        }

        when (val parsed = parseCredentialPayload(raw)) {
            is QrParseResult.Invalid -> _uiState.update {
                it.copy(phase = ScanPhase.SCANNING, scanError = parsed.reason)
            }

            is QrParseResult.Valid -> {
                _uiState.update {
                    it.copy(
                        phase = ScanPhase.LOOKING_UP,
                        scanError = null,
                        errorMessage = null,
                        employee = null,
                        rawQr = raw,
                        pendingClientEventId = newClientEventId(),
                    )
                }
                viewModelScope.launch {
                    val result = runCatching { accessApi.lookup(raw) }
                    result.fold(
                        onSuccess = { employee ->
                            _uiState.update {
                                it.copy(
                                    phase = ScanPhase.REVIEW,
                                    employee = employee,
                                    selectedType = employee.suggestedType
                                        .ifBlank { suggestedTypeFromLastEvent(employee.lastEvent) },
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update {
                                it.copy(
                                    phase = ScanPhase.SCANNING,
                                    scanError = networkMessage(e),
                                    pendingClientEventId = null,
                                    rawQr = null,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    fun setType(type: String) = _uiState.update { it.copy(selectedType = type) }

    fun confirm() = register()

    /** Reintenta el MISMO escaneo: conserva `clientEventId` (idempotencia). */
    fun retryRegistration() {
        if (_uiState.value.errorMessage == null) return
        register()
    }

    private fun register() {
        val state = _uiState.value
        if (state.submitting) return
        val siteId = state.selectedSiteId
        val employee = state.employee
        val clientEventId = state.pendingClientEventId
        if (siteId.isNullOrBlank() || employee == null || clientEventId.isNullOrBlank()) return

        _uiState.update { it.copy(submitting = true, errorMessage = null) }
        viewModelScope.launch {
            val location = runCatching { locationProvider.currentLocation() }.getOrNull()
            val result = runCatching {
                accessApi.createEvent(
                    AccessEventInput(
                        qr = state.rawQr,
                        employeeId = employee.id,
                        type = state.selectedType,
                        siteId = siteId,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        accuracy = location?.accuracy,
                        deviceTimestamp = formatIsoUtc(currentTimeMillis()),
                        clientEventId = clientEventId,
                    )
                )
            }
            result.fold(
                onSuccess = { event ->
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            phase = ScanPhase.SUCCESS,
                            result = event,
                            errorMessage = null,
                            locationSource = if (location != null) "GPS" else "SITE_ONLY",
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(submitting = false, errorMessage = networkMessage(e)) }
                },
            )
        }
    }

    /** Cierra el resultado y deja la cámara lista para el siguiente empleado. */
    fun scanNext() {
        _uiState.update {
            it.copy(
                phase = ScanPhase.SCANNING,
                employee = null,
                result = null,
                errorMessage = null,
                scanError = null,
                selectedType = AccessEventType.ENTRY,
                pendingClientEventId = null,
                rawQr = null,
                locationSource = null,
            )
        }
    }

    /** Descarta la revisión en curso y vuelve a escanear. */
    fun cancelReview() {
        _uiState.update {
            it.copy(
                phase = ScanPhase.SCANNING,
                employee = null,
                errorMessage = null,
                scanError = null,
                pendingClientEventId = null,
                rawQr = null,
            )
        }
    }

    fun dismissScanError() = _uiState.update { it.copy(scanError = null) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
