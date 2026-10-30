package com.axzydev.puertonuevoapp.feature.timeclock

import com.axzydev.puertonuevoapp.core.network.timeclock.EmployeeClockStatusDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockEmployeeDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockEmployeesSummaryDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto

/**
 * Estado de la pantalla del padrón del reloj (la tabla en sí la lleva
 * `TableViewModelBase`/`TableUiState`). Aquí vive el resumen que pintan los
 * mosaicos, el diálogo de vincular/cambiar y el detalle de estado en los relojes.
 */
data class TimeClockEmployeesUiState(
    /** Totales del padrón completo (no del filtro), como los manda el API. */
    val summary: TimeClockEmployeesSummaryDto = TimeClockEmployeesSummaryDto(),
    /** Cuentas activas para el selector de vínculo (solo si puede vincular). */
    val users: List<UserDto> = emptyList(),
    val loadingUsers: Boolean = false,
    /** Fila cuyo vínculo se está editando; `null` = diálogo cerrado. */
    val linkingTarget: TimeClockEmployeeDto? = null,
    /** Cuenta elegida en el diálogo. */
    val selectedUserId: String = "",
    /** Número del reloj cuya fila está guardando (deshabilita solo esa fila). */
    val savingNumber: String? = null,
    /** Guardando desde el diálogo de vincular (deshabilita los botones del modal). */
    val savingLink: Boolean = false,
    /** Corriendo el vínculo masivo de sugerencias seguras. */
    val linkingSuggested: Boolean = false,
    /** Persona vinculada cuyo estado en cada reloj se está consultando. */
    val clockStatusTarget: TimeClockEmployeeDto? = null,
    val clockStatus: EmployeeClockStatusDto? = null,
    val loadingClockStatus: Boolean = false,
)

/** Clave del filtro de estado de la tabla (también la usan los mosaicos). */
const val TimeClockEmployeeStatusKey: String = "status"

/**
 * Estado de vínculo de una fila, mismo criterio que la web: con vínculo
 * `LINKED`; sin vínculo pero con candidato `SUGGESTED`; si no, `UNLINKED`.
 */
fun TimeClockEmployeeDto.linkStatus(): String = when {
    link != null -> "LINKED"
    suggestion != null -> "SUGGESTED"
    else -> "UNLINKED"
}
