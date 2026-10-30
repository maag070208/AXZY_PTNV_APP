package com.axzydev.puertonuevoapp.feature.timeclock

import com.axzydev.puertonuevoapp.core.network.timeclock.RemovalCategoryDto
import com.axzydev.puertonuevoapp.core.network.timeclock.RosterRunResultDto
import com.axzydev.puertonuevoapp.core.network.timeclock.TimeClockRosterDto

/** Longitud mínima del motivo de baja (misma regla que la web). */
const val MinRemovalReasonLength: Int = 3

/**
 * Borrador del motivo de baja de una persona con baja pendiente: categoría del
 * catálogo + detalle. No se envía nada hasta que RH confirma.
 */
data class RosterRemovalDraft(
    val categoryId: String = "",
    val reason: String = "",
) {
    /** Mismo criterio que la web: categoría elegida y motivo de al menos 3 caracteres. */
    val isValid: Boolean
        get() = categoryId.isNotBlank() && reason.trim().length >= MinRemovalReasonLength
}

/**
 * Estado de la pantalla del rol de turnos (padrón del reloj).
 *
 * La API solo expone la **última corrida** guardada (`roster().lastRun`) y el
 * resultado de una revisión manual (`runRoster()`); no hay endpoint de
 * historial de corridas, así que aquí se conserva el resultado de la última
 * revisión hecha desde la app junto con la corrida registrada.
 */
data class TimeClockRosterUiState(
    /** Padrón vigente: nuevos, bajas pendientes y reapariciones. */
    val roster: TimeClockRosterDto? = null,
    /** Catálogo de motivos de baja (activos). */
    val categories: List<RemovalCategoryDto> = emptyList(),
    /** Cargando por primera vez (pantalla completa). */
    val loading: Boolean = true,
    /** Recargando con datos ya en pantalla. */
    val refreshing: Boolean = false,
    /** Error de carga del padrón (con datos en pantalla se muestra como aviso). */
    val error: String? = null,
    /** Corriendo "Revisar padrón" (solo lee los relojes). */
    val running: Boolean = false,
    /** Resultado de la última revisión manual hecha desde la app. */
    val lastManualRun: RosterRunResultDto? = null,
    /** Número del reloj cuya baja se está aplicando/descartando. */
    val busyNumber: String? = null,
    /** Borradores de motivo por número de empleado del reloj. */
    val drafts: Map<String, RosterRemovalDraft> = emptyMap(),
) {
    fun draftOf(employeeNumber: String): RosterRemovalDraft =
        drafts[employeeNumber] ?: RosterRemovalDraft()
}
