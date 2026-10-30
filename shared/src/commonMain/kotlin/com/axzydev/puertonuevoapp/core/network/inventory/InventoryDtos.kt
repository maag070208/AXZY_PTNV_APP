package com.axzydev.puertonuevoapp.core.network.inventory

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import kotlinx.serialization.Serializable

@Serializable
data class InventoryMovementDto(
    val id: String,
    val deviceId: String,
    val device: DeviceDto? = null,
    val locationId: String? = null,
    val location: LocationDto? = null,
    val type: String,
    /** `ACTIVE` o `CANCELLED` (`MovementStatus` del API; una reversión cancela el origen). */
    val status: String = "ACTIVE",
    val notes: String? = null,
    val userId: String,
    val user: UserRefDto? = null,
    val loanId: String? = null,
    val loanedTo: String? = null,
    val expectedReturnDate: String? = null,
    val condition: String? = null,
    val retirementReason: String? = null,
    val createdAt: String,
    /** "A-0001 · Laptop Dell" (armado del `items[0].device` del API: código y nombre). */
    val deviceLabel: String? = null,
    /** "A-0001, A-0002" (activos fijos de `items[0].units`) o "×3" si sólo hay cantidad. */
    val unitsLabel: String? = null,
)

@Serializable
data class StockLedgerDto(
    val device: DeviceDto,
    val movements: List<InventoryMovementDto> = emptyList(),
)

@Serializable
data class InventoryStatsDto(
    val totalDevices: Int = 0,
    val locatedDevices: Int = 0,
    val unlocatedDevices: Int = 0,
)

@Serializable
data class InventorySummaryDto(
    val locations: List<LocationDto> = emptyList(),
    val stats: InventoryStatsDto = InventoryStatsDto(),
)

@Serializable
data class MovementInput(
    val deviceId: String,
    val type: String,
    val locationId: String? = null,
    val notes: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val loanId: String? = null,
    val loanedTo: String? = null,
    val expectedReturnDate: String? = null,
    val condition: String? = null,
    val retirementReason: String? = null,
    val custodyLetterId: String? = null,
    /**
     * Piezas que mueve la operación. En `STOCK_IN` son las piezas NUEVAS que se
     * crean (tope 5000 por renglón); en el resto de tipos la API usa la cantidad
     * o las unidades exactas (`unitIds`).
     */
    val quantity: Int = 1,
    /**
     * Identidades de las piezas NUEVAS de una entrada (`STOCK_IN`), en el orden
     * en que se crean. Es opcional y puede venir incompleta: las piezas sin
     * datos se crean vacías y se capturan después desde el detalle.
     */
    val units: List<UnitIdentityInput> = emptyList(),
)

val movementTypeOptions: List<Pair<String, String>> = listOf(
    "STOCK_IN" to "Entrada",
    "STOCK_OUT" to "Salida",
    "TRANSFER" to "Traslado",
    "RETIREMENT" to "Baja",
    "LOAN" to "Asignado",
    "RETURN" to "Devolución",
)

val conditionOptions: List<Pair<String, String>> = listOf(
    "GOOD" to "Bueno",
    "FAIR" to "Aceptable",
    "POOR" to "Malo",
    "BROKEN" to "Roto",
)

// ---------------------------------------------------------------------------
// Fase 1 del plan de paridad: unidades físicas, existencias, kardex,
// movimientos, devoluciones, auditor y detalle de préstamo.
//
// Los `String` de tipo/estado/condición son los enums del API
// (`MovementType`, `DeviceUnitStatus`, `Condition`, `LoanStatus`); se dejan como
// texto por consistencia con el resto de la app (ver `movementTypeOptions`).
// ---------------------------------------------------------------------------

/** Referencia liviana al dispositivo dentro de un movimiento, una devolución o el kardex. */
@Serializable
data class InventoryDeviceRefDto(
    val id: String,
    val code: String? = null,
    val typeId: String = "",
    val type: InventoryDeviceTypeRefDto? = null,
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val description: String? = null,
    val notes: String? = null,
)

/** Referencia liviana al tipo del dispositivo embebido en [InventoryDeviceRefDto]. */
@Serializable
data class InventoryDeviceTypeRefDto(
    val id: String,
    val code: String = "",
    val name: String = "",
    val assetTagPrefix: String = "",
)

/**
 * Una pieza física (`DeviceUnit`): `GET /inventory/devices/:id/units`.
 * En la búsqueda (`GET /inventory/units`) llegan además [department] y [device].
 */
@Serializable
data class DeviceUnitDto(
    val id: String,
    val deviceId: String = "",
    val assetTag: String = "",
    val serialNumber: String? = null,
    val macAddress: String? = null,
    val ip: String? = null,
    val hostname: String? = null,
    val area: String = "",
    val status: String = "AVAILABLE",
    val departmentId: String? = null,
    /** Sólo en `GET /inventory/units`: departamento al que está asignada la pieza. */
    val department: DepartmentRefDto? = null,
    /** Sólo en `GET /inventory/units`: dispositivo al que pertenece la pieza. */
    val device: InventoryDeviceRefDto? = null,
)

/** Identificación de una pieza nueva (serie/MAC/IP/hostname) al dar de alta unidades. */
@Serializable
data class UnitIdentityInput(
    val serialNumber: String? = null,
    val macAddress: String? = null,
    val ip: String? = null,
    val hostname: String? = null,
)

/**
 * Datos editables de una pieza (`PUT /inventory/units/:id`). El departamento NO
 * se cambia aquí: se mueve con un traspaso (así queda en el kardex y la bitácora).
 */
@Serializable
data class UnitUpdateInput(
    val serialNumber: String? = null,
    val macAddress: String? = null,
    val ip: String? = null,
    val hostname: String? = null,
    val area: String? = null,
)

/**
 * Existencias de un dispositivo (`GET /inventory/devices/:id/stock`).
 * `active` = disponible + prestado + dañado + mantenimiento; `historical` suma
 * además las retiradas (la misma regla del kardex).
 */
@Serializable
data class StockDto(
    val AVAILABLE: Int = 0,
    val ON_LOAN: Int = 0,
    val DAMAGED: Int = 0,
    val IN_MAINTENANCE: Int = 0,
    val RETIRED: Int = 0,
    val active: Int = 0,
    val historical: Int = 0,
)

/** Kardex de un dispositivo (`GET /inventory/devices/:id/ledger`); el endpoint NO pagina. */
@Serializable
data class DeviceLedgerDto(
    val device: InventoryDeviceRefDto? = null,
    val stock: StockDto = StockDto(),
    val rows: List<StockLedgerRowDto> = emptyList(),
)

/** Un renglón del kardex: lo que entró/salió y el saldo (existencia disponible) tras él. */
@Serializable
data class StockLedgerRowDto(
    val date: String = "",
    val type: String = "",
    val stockIn: Int = 0,
    val stockOut: Int = 0,
    val balance: Int = 0,
    val condition: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val user: String? = null,
)

/** Referencia liviana a la persona que creó/custodia un movimiento o préstamo. */
@Serializable
data class InventoryUserRefDto(
    val id: String = "",
    val name: String = "",
)

/** Un movimiento del inventario (`Movement` del API), con sus renglones y piezas. */
@Serializable
data class MovementDto(
    val id: String,
    val type: String = "",
    val date: String = "",
    val createdById: String = "",
    val createdBy: InventoryUserRefDto? = null,
    val custodian: InventoryUserRefDto? = null,
    val departmentId: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val status: String = "ACTIVE",
    val reversalOfId: String? = null,
    val loanId: String? = null,
    val items: List<MovementItemDto> = emptyList(),
)

/** Un renglón de un movimiento: cantidad movida y las piezas exactas ligadas. */
@Serializable
data class MovementItemDto(
    val id: String = "",
    val deviceId: String = "",
    val device: InventoryDeviceRefDto? = null,
    val quantity: Int = 0,
    val condition: String? = null,
    val notes: String? = null,
    val units: List<MovementUnitDto> = emptyList(),
)

/** Pieza ligada a un renglón de movimiento. */
@Serializable
data class MovementUnitDto(
    val id: String = "",
    val deviceUnitId: String = "",
    val deviceUnit: DeviceUnitDto? = null,
)

/** Un renglón de una devolución (`POST /inventory/returns`). */
@Serializable
data class ReturnCreateItemInput(
    val loanItemId: String,
    /** Piezas que regresan de ese renglón. Debe venir `quantity` o `unitIds`. */
    val quantity: Int? = null,
    /** Unidades exactas pendientes que regresan con esta condición. */
    val unitIds: List<String>? = null,
    /** `GOOD` | `FAIR` | `POOR` | `BROKEN`. */
    val condition: String = "GOOD",
    val notes: String? = null,
)

/** Cuerpo de `POST /inventory/returns` (devolución parcial por pieza de un préstamo). */
@Serializable
data class ReturnCreateInput(
    val loanId: String,
    val custodianId: String? = null,
    val notes: String? = null,
    val items: List<ReturnCreateItemInput> = emptyList(),
)

/** Referencia liviana a una persona o departamento dentro de una devolución. */
@Serializable
data class InventoryNameRefDto(
    val id: String? = null,
    val name: String = "",
)

/** Préstamo al que pertenece una devolución (lo que trae `GET /inventory/returns`). */
@Serializable
data class LoanReturnLoanRefDto(
    val id: String = "",
    val number: String = "",
    val custodian: InventoryNameRefDto? = null,
    val department: InventoryNameRefDto? = null,
)

/** Una devolución registrada (`GET /inventory/returns`). */
@Serializable
data class LoanReturnDto(
    val id: String,
    val loanId: String = "",
    val loan: LoanReturnLoanRefDto? = null,
    val date: String = "",
    val custodianId: String? = null,
    val number: String = "",
    val notes: String? = null,
    val items: List<LoanReturnItemDto> = emptyList(),
)

/** Un renglón de la devolución, con su condición y las piezas que volvieron. */
@Serializable
data class LoanReturnItemDto(
    val id: String,
    val deviceId: String = "",
    val device: InventoryDeviceRefDto? = null,
    val quantity: Int = 0,
    val condition: String = "GOOD",
    val notes: String? = null,
    val units: List<LoanItemUnitDto> = emptyList(),
)

/**
 * Pieza física dentro de un renglón de préstamo (`GET /inventory/loans/:id`).
 * El API sólo guarda `returned`; la condición viaja en la devolución
 * ([LoanReturnItemDto.condition]), no en la pieza.
 */
@Serializable
data class LoanItemUnitDto(
    val id: String = "",
    val deviceUnitId: String = "",
    val returned: Boolean = false,
    val deviceUnit: DeviceUnitDto? = null,
)

/** Un renglón del préstamo con sus piezas exactas (para devolver por pieza). */
@Serializable
data class LoanItemDto(
    val id: String,
    val deviceId: String = "",
    val device: InventoryDeviceRefDto? = null,
    val quantity: Int = 0,
    val returnedQuantity: Int = 0,
    val notes: String? = null,
    val units: List<LoanItemUnitDto> = emptyList(),
) {
    /** Piezas del renglón que siguen sin devolverse. */
    val pending: Int get() = (quantity - returnedQuantity).coerceAtLeast(0)
}

/**
 * Detalle de un préstamo/carta (`GET /inventory/loans/:id`). El folio visible es
 * [number] (`CUSTODY_LETTER-####`); [date] es la fecha de entrega.
 */
@Serializable
data class LoanDetailDto(
    val id: String,
    val custodianId: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
    val status: String = "ACTIVE",
    val number: String = "",
    val date: String = "",
    val createdAt: String? = null,
    val notes: String? = null,
    val custodian: InventoryNameRefDto? = null,
    val department: DepartmentRefDto? = null,
    val subarea: DepartmentRefDto? = null,
    val items: List<LoanItemDto> = emptyList(),
    val returns: List<LoanReturnDto> = emptyList(),
)

/**
 * Resultado del auditor de consistencia (`GET /inventory/audit`): cada una de
 * las 8 reglas con cuántos casos la rompen y ejemplos legibles.
 */
@Serializable
data class InventoryAuditDto(
    val ok: Boolean = false,
    val checkedAt: String = "",
    val checks: List<InventoryAuditCheckDto> = emptyList(),
)

/** Una regla del auditor con su conteo, ejemplos y —si aplica— el detalle en piezas. */
@Serializable
data class InventoryAuditCheckDto(
    val key: String = "",
    val count: Int = 0,
    val samples: List<String> = emptyList(),
    val rows: List<InventoryAuditRowDto> = emptyList(),
)

/**
 * Caso de descuadre en piezas. `movementType` viene ya traducido por el API
 * (texto legible, no el enum); `linked`/`quantity` explican el renglón y
 * `ledger`/`available` la existencia del dispositivo hoy.
 */
@Serializable
data class InventoryAuditRowDto(
    val movementItemId: String? = null,
    val movementType: String? = null,
    val date: String? = null,
    val device: String? = null,
    val deviceId: String? = null,
    val quantity: Int? = null,
    val linked: Int? = null,
    val ledger: Int? = null,
    val available: Int? = null,
    val linkedAfter: Int? = null,
    val ledgerIfQuantity: Int? = null,
)

/**
 * `POST /inventory/audit/mismatches/resolve`: cómo resolver un descuadre.
 * `link` liga las piezas que existan; `quantity` liga lo que haya y deja la
 * cantidad del renglón en lo ligado (MUEVE el kardex); `review` sólo deja
 * constancia de que se revisó.
 */
@Serializable
data class ResolveMismatchInput(
    val movementItemId: String,
    /** `link` | `quantity` | `review`. */
    val mode: String,
)

/** Resultado de resolver un descuadre. */
@Serializable
data class ResolveMismatchResultDto(
    val movementItemId: String = "",
    val linked: Int = 0,
    val remaining: Int = 0,
    val quantity: Int = 0,
)
