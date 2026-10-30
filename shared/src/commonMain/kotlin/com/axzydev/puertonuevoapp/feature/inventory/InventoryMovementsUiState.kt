package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.inventory.InventoryMovementDto
import com.axzydev.puertonuevoapp.core.ui.table.DateRangeFromSuffix
import com.axzydev.puertonuevoapp.core.ui.table.DateRangeToSuffix

/** Claves de `TableUiState.filters` que usa el kardex (las comparten pantalla y ViewModel). */
internal object MovementFilterKeys {
    const val Search = "q"
    const val Type = "type"
    const val Device = "device"
    const val User = "user"
    const val Date = "date"
    val DateFrom: String = Date + DateRangeFromSuffix
    val DateTo: String = Date + DateRangeToSuffix
}

/**
 * La API solo revierte movimientos de mantenimiento (`movementReversion` en
 * `inventory.service.ts`); el resto responde 409 `MOVEMENT_NOT_REVERSIBLE` y uno
 * ya revertido 409 `MOVEMENT_ALREADY_REVERSED`.
 */
val InventoryMovementRow.isReversible: Boolean
    get() = type == "MAINTENANCE_IN" || type == "MAINTENANCE_OUT"

/**
 * Fila aplanada del kardex para el estándar `AppTableScreen`. Un movimiento = una
 * fila, con la etiqueta del primer renglón (`items[0]`: dispositivo y activos fijos)
 * que expone `InventoryMovementDto`.
 */
data class InventoryMovementRow(
    val id: String,
    val date: String,
    val type: String,
    val deviceId: String,
    val deviceLabel: String,
    val unitsLabel: String,
    val reason: String?,
    val userLabel: String,
) {
    /** Texto único de la búsqueda libre del estándar (clave `q`). */
    val searchText: String
        get() = listOf(date, type, deviceId, deviceLabel, unitsLabel, reason.orEmpty(), userLabel)
            .joinToString(" ")
            .lowercase()

    /** ¿El día del movimiento (YYYY-MM-DD) cae en el rango? Los extremos son opcionales. */
    fun inDayRange(from: String?, to: String?): Boolean {
        val day = date.take(10)
        if (!from.isNullOrBlank() && day < from) return false
        if (!to.isNullOrBlank() && day > to) return false
        return true
    }
}

internal fun InventoryMovementDto.toRow(): InventoryMovementRow {
    // `deviceLabel`/`unitsLabel` ya vienen armados por InventoryApi a partir de
    // `items[0].device` y `items[0].units`; si el API no los manda, se cae a "—".
    val fallbackDevice = device?.let { d ->
        listOfNotNull(
            d.assetTag.takeIf { it.isNotBlank() },
            d.description.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { null }
    }
    return InventoryMovementRow(
        id = id,
        date = createdAt,
        type = type,
        deviceId = deviceId,
        deviceLabel = this.deviceLabel?.takeIf { it.isNotBlank() } ?: fallbackDevice ?: "—",
        unitsLabel = this.unitsLabel?.takeIf { it.isNotBlank() } ?: "—",
        reason = retirementReason ?: notes,
        userLabel = user?.name ?: "—",
    )
}
