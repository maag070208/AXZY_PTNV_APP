package com.axzydev.puertonuevoapp.feature.devices

import com.axzydev.puertonuevoapp.core.network.devices.DeviceCreateUnitInput
import com.axzydev.puertonuevoapp.core.network.devices.MAX_DEVICE_INITIAL_QUANTITY
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypeDto

/** Campos de identificación que el tipo del dispositivo pide pieza por pieza. */
enum class UnitIdentityField { SerialNumber, Mac, Ip, Hostname }

/**
 * Identificación de UNA pieza física mientras se captura el alta por lote.
 * El [id] es de la pantalla (clave estable de la fila), no de la base.
 */
data class UnitIdentityRow(
    val id: Long,
    val serialNumber: String = "",
    val macAddress: String = "",
    val ip: String = "",
    val hostname: String = "",
) {
    val filled: Boolean
        get() = serialNumber.isNotBlank() || macAddress.isNotBlank() || ip.isNotBlank() || hostname.isNotBlank()

    fun identity(field: UnitIdentityField): String = when (field) {
        UnitIdentityField.SerialNumber -> serialNumber
        UnitIdentityField.Mac -> macAddress
        UnitIdentityField.Ip -> ip
        UnitIdentityField.Hostname -> hostname
    }

    fun withIdentity(field: UnitIdentityField, value: String): UnitIdentityRow = when (field) {
        UnitIdentityField.SerialNumber -> copy(serialNumber = value)
        UnitIdentityField.Mac -> copy(macAddress = value)
        UnitIdentityField.Ip -> copy(ip = value)
        UnitIdentityField.Hostname -> copy(hostname = value)
    }

    /** Lo que viaja al API: los campos vacíos se omiten (la pieza se captura después). */
    fun toInput(): DeviceCreateUnitInput = DeviceCreateUnitInput(
        serialNumber = serialNumber.trim().ifBlank { null },
        macAddress = macAddress.trim().ifBlank { null },
        ip = ip.trim().ifBlank { null },
        hostname = hostname.trim().ifBlank { null },
    )
}

data class DeviceFormUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val types: List<DeviceTypeDto> = emptyList(),
    val typeId: String = "",
    val description: String = "",
    val brand: String = "",
    val model: String = "",
    val area: String = "SISTEMAS",
    val serialNumber: String = "",
    val hostname: String = "",
    val ip: String = "",
    val macAddress: String = "",
    val operatingSystem: String = "",
    val ram: String = "",
    val storage: String = "",
    val blocked: Boolean = false,
    val blockedCode: String = "",
    val saved: Boolean = false,
    /** true = alta (permite N piezas); false = edición (comportamiento de siempre). */
    val creating: Boolean = true,
    /** Cantidad de piezas del alta, como texto para no pelear con el teclado. */
    val quantityText: String = "1",
    /** Un renglón por pieza, EN EL ORDEN en que se crean. */
    val unitRows: List<UnitIdentityRow> = listOf(UnitIdentityRow(id = 0L)),
    val expandedUnitIds: Set<Long> = setOf(0L),
) {
    private val selectedType: DeviceTypeDto? get() = types.firstOrNull { it.id == typeId }

    fun showField(key: String): Boolean = when (key) {
        "serialNumber" -> selectedType?.fieldConfig?.serialNumber?.enabled ?: true
        "hostname" -> selectedType?.fieldConfig?.hostname?.enabled ?: true
        "ip" -> selectedType?.fieldConfig?.ip?.enabled ?: false
        "macAddress" -> selectedType?.fieldConfig?.macAddress?.enabled ?: false
        "operatingSystem" -> selectedType?.fieldConfig?.operatingSystem?.enabled ?: false
        "ram" -> selectedType?.fieldConfig?.ram?.enabled ?: false
        "storage" -> selectedType?.fieldConfig?.storage?.enabled ?: false
        else -> false
    }

    val quantity: Int get() = quantityText.toIntOrNull() ?: 0
    val quantityValid: Boolean get() = quantity in 1..MAX_DEVICE_INITIAL_QUANTITY

    /** ¿El tipo pide serie/MAC/IP/hostname? Si no, no hay editor por pieza. */
    val usesUnitIdentity: Boolean
        get() = showField("serialNumber") || showField("hostname") || showField("ip") || showField("macAddress")

    val showSpecs: Boolean get() = listOf("ip", "macAddress", "operatingSystem", "ram", "storage").any(::showField)

    val isValid: Boolean
        get() = typeId.isNotBlank() && description.isNotBlank() && brand.isNotBlank() && model.isNotBlank() &&
            (!creating || (quantityValid && unitRows.size == quantity))

    /** Primer folio que consumirá el alta (el API lo calcula con el contador del tipo). */
    val firstFolio: String?
        get() = selectedType?.let { type ->
            "${type.prefix}-${(type.counter + 1).toString().padStart(4, '0')}"
        }

    /** Último folio esperado del lote, para la vista previa. */
    val lastFolio: String?
        get() = selectedType?.let { type ->
            "${type.prefix}-${(type.counter + quantity.coerceAtLeast(1)).toString().padStart(4, '0')}"
        }

    val folioHint: String?
        get() = when {
            !creating || !quantityValid -> null
            quantity <= 1 -> firstFolio?.let { "Se generará el folio $it." }
            else -> if (firstFolio != null && lastFolio != null) {
                "Se generarán $quantity folios: de $firstFolio a $lastFolio."
            } else {
                null
            }
        }

    /** Cuántas piezas ya tienen algún dato de identificación. */
    val identifiedCount: Int get() = unitRows.count { it.filled }
}
