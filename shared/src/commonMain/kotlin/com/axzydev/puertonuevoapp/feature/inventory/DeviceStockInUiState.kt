package com.axzydev.puertonuevoapp.feature.inventory

import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto
import com.axzydev.puertonuevoapp.core.network.inventory.StockDto
import com.axzydev.puertonuevoapp.core.network.inventory.UnitIdentityInput

/** Tope del API para una entrada de piezas nuevas por renglón. */
const val MAX_STOCK_IN_QUANTITY = 5000

/** Campos de identificación que el tipo del dispositivo pide pieza por pieza. */
enum class StockInIdentityField { SerialNumber, Mac, Ip, Hostname }

/**
 * Identificación de UNA pieza nueva mientras se captura la entrada. El [id] es
 * de la pantalla (clave estable de la fila), no de la base.
 */
data class StockInIdentityRow(
    val id: Long,
    val serialNumber: String = "",
    val macAddress: String = "",
    val ip: String = "",
    val hostname: String = "",
) {
    val filled: Boolean
        get() = serialNumber.isNotBlank() || macAddress.isNotBlank() || ip.isNotBlank() || hostname.isNotBlank()

    fun identity(field: StockInIdentityField): String = when (field) {
        StockInIdentityField.SerialNumber -> serialNumber
        StockInIdentityField.Mac -> macAddress
        StockInIdentityField.Ip -> ip
        StockInIdentityField.Hostname -> hostname
    }

    fun withIdentity(field: StockInIdentityField, value: String): StockInIdentityRow = when (field) {
        StockInIdentityField.SerialNumber -> copy(serialNumber = value)
        StockInIdentityField.Mac -> copy(macAddress = value)
        StockInIdentityField.Ip -> copy(ip = value)
        StockInIdentityField.Hostname -> copy(hostname = value)
    }

    /** Lo que viaja al API: los campos vacíos se omiten (la pieza se captura después). */
    fun toInput(): UnitIdentityInput = UnitIdentityInput(
        serialNumber = serialNumber.trim().ifBlank { null },
        macAddress = macAddress.trim().ifBlank { null },
        ip = ip.trim().ifBlank { null },
        hostname = hostname.trim().ifBlank { null },
    )
}

/** Resultado de una entrada registrada, para la pantalla de éxito. */
data class StockInResult(
    val quantity: Int,
    val firstFolio: String?,
    val lastFolio: String?,
    val reason: String,
)

data class DeviceStockInUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    /** Dispositivo que recibe las piezas (llega por parámetro de navegación). */
    val device: DeviceDto? = null,
    /** Existencias actuales, solo informativas (si fallan, la entrada se puede registrar igual). */
    val stock: StockDto? = null,
    /** Piezas nuevas a crear, como texto para no pelear con el teclado. */
    val quantityText: String = "1",
    /** Motivo de la entrada (el `reason` del movimiento), opcional. */
    val reason: String = "",
    val notes: String = "",
    /** Un renglón por pieza, EN EL ORDEN en que se crean. */
    val rows: List<StockInIdentityRow> = listOf(StockInIdentityRow(id = 0L)),
    val expandedRowIds: Set<Long> = setOf(0L),
    val saving: Boolean = false,
    val submitError: String? = null,
    /** No nulo = la entrada ya se registró (pantalla de éxito). */
    val result: StockInResult? = null,
) {
    val type: DeviceTypeDto? get() = device?.type

    fun showField(key: String): Boolean = when (key) {
        "serialNumber" -> type?.fieldConfig?.serialNumber?.enabled ?: false
        "hostname" -> type?.fieldConfig?.hostname?.enabled ?: false
        "ip" -> type?.fieldConfig?.ip?.enabled ?: false
        "macAddress" -> type?.fieldConfig?.macAddress?.enabled ?: false
        else -> false
    }

    /** ¿El tipo pide serie/MAC/IP/hostname? Si no, no hay editor por pieza. */
    val usesUnitIdentity: Boolean
        get() = showField("serialNumber") || showField("hostname") || showField("ip") || showField("macAddress")

    val quantity: Int get() = quantityText.toIntOrNull() ?: 0
    val quantityValid: Boolean get() = quantity in 1..MAX_STOCK_IN_QUANTITY
    val canSubmit: Boolean get() = device != null && quantityValid && !saving
    val identifiedCount: Int get() = rows.count { it.filled }

    val available: Int get() = stock?.AVAILABLE ?: 0
    val active: Int get() = stock?.active ?: 0

    /** Primer folio que consumirá la entrada (el API los toma del contador del tipo). */
    val firstFolio: String?
        get() = type?.let { "${it.prefix}-${(it.counter + 1).toString().padStart(4, '0')}" }

    /** Último folio esperado del lote, para la vista previa. */
    val lastFolio: String?
        get() = type?.let { "${it.prefix}-${(it.counter + quantity.coerceAtLeast(1)).toString().padStart(4, '0')}" }

    val folioHint: String?
        get() = when {
            !quantityValid -> null
            quantity <= 1 -> firstFolio?.let { "Se creará la pieza con folio $it." }
            else -> if (firstFolio != null && lastFolio != null) {
                "Se crearán $quantity piezas: de $firstFolio a $lastFolio."
            } else {
                null
            }
        }
}
