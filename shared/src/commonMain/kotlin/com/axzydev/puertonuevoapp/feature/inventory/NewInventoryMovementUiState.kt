package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto

/**
 * Formulario de movimiento de inventario. Además de los tipos "de una pieza"
 * (salida, traslado, baja, asignación, devolución), el tipo **Entrada**
 * (`STOCK_IN`) captura piezas NUEVAS: cantidad (1..5000) e identidades
 * opcionales por pieza, igual que la web (`NewMovementPage`) y que
 * "Agregar unidades" ([DeviceStockInUiState]); la entrada ya no exige ubicación.
 */
data class NewInventoryMovementUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val devices: List<DeviceDto> = emptyList(),
    val locations: List<LocationDto> = emptyList(),
    val deviceId: String = "",
    val type: String = "",
    val locationId: String = "",
    val notes: String = "",
    val loanedTo: String = "",
    val returnDate: String = "",
    val condition: String = "",
    val retirementReason: String = "",
    val poorConditionAction: String = "",
    val saved: Boolean = false,
    /** Solo para `STOCK_IN`: piezas nuevas a crear (texto para el teclado numérico). */
    val quantityText: String = "1",
    /** Solo para `STOCK_IN`: motivo de la entrada (el `reason` del movimiento). */
    val entryReason: String = "",
    /** Solo para `STOCK_IN`: identificaciones capturadas, EN ORDEN de creación. */
    val identityRows: List<StockInIdentityRow> = emptyList(),
) {
    val selectedDevice: DeviceDto? get() = devices.firstOrNull { it.id == deviceId }

    val isEntry: Boolean get() = type == "STOCK_IN"

    /**
     * La entrada de piezas nuevas ya no exige ubicación (como la web: las piezas
     * se ligan al dispositivo); el traslado sí necesita ubicación destino.
     */
    val requiresLocation: Boolean get() = type == "TRANSFER"

    val requiresLoan: Boolean get() = type == "LOAN"
    val requiresReturn: Boolean get() = type == "RETURN"
    val isPoorCondition: Boolean get() = condition == "POOR" || condition == "BROKEN"

    // --- Entrada (STOCK_IN) -------------------------------------------------
    val quantity: Int get() = quantityText.toIntOrNull() ?: 0
    val quantityValid: Boolean get() = quantity in 1..MAX_STOCK_IN_QUANTITY
    val identityCount: Int get() = identityRows.count { it.filled }
    val canAddIdentity: Boolean get() = isEntry && identityRows.size < quantity

    /** ¿El tipo del dispositivo pide serie/MAC/IP/hostname por pieza? */
    fun showField(key: String): Boolean = when (key) {
        "serialNumber" -> selectedDevice?.type?.fieldConfig?.serialNumber?.enabled ?: false
        "hostname" -> selectedDevice?.type?.fieldConfig?.hostname?.enabled ?: false
        "ip" -> selectedDevice?.type?.fieldConfig?.ip?.enabled ?: false
        "macAddress" -> selectedDevice?.type?.fieldConfig?.macAddress?.enabled ?: false
        else -> false
    }

    val usesUnitIdentity: Boolean
        get() = showField("serialNumber") || showField("hostname") || showField("ip") || showField("macAddress")

    /** Primer folio que consumirá la entrada (el API los toma del contador del tipo). */
    val firstFolio: String?
        get() = selectedDevice?.type?.let { "${it.prefix}-${(it.counter + 1).toString().padStart(4, '0')}" }

    /** Último folio esperado del lote, para la vista previa. */
    val lastFolio: String?
        get() = selectedDevice?.type?.let {
            "${it.prefix}-${(it.counter + quantity.coerceAtLeast(1)).toString().padStart(4, '0')}"
        }

    val folioHint: String?
        get() = when {
            !isEntry -> null
            !quantityValid -> null
            quantity <= 1 -> firstFolio?.let { "Se creará la pieza con folio $it." }
            else -> if (firstFolio != null && lastFolio != null) {
                "Se crearán $quantity piezas: de $firstFolio a $lastFolio."
            } else {
                null
            }
        }

    val isValid: Boolean
        get() = deviceId.isNotBlank() && type.isNotBlank() &&
            (!isEntry || quantityValid) &&
            (!requiresLocation || locationId.isNotBlank()) &&
            (!requiresLoan || (loanedTo.isNotBlank() && returnDate.isNotBlank())) &&
            (!requiresReturn || condition.isNotBlank()) &&
            (!isPoorCondition || poorConditionAction.isNotBlank())
}

val movementTypeFormOptions: List<Pair<String, String>> = listOf(
    "STOCK_IN" to "Entrada (alta de piezas nuevas)",
    "STOCK_OUT" to "Salida (retirar de ubicación)",
    "TRANSFER" to "Traslado (mover a otra ubicación)",
    "RETIREMENT" to "Baja (dar de baja el dispositivo)",
    "LOAN" to "Asignado (equipo entregado a alguien)",
    "RETURN" to "Devolución (equipo regresado de asignación)",
)

val movementConditionCodes: List<String> = listOf("GOOD", "FAIR", "POOR", "BROKEN")
