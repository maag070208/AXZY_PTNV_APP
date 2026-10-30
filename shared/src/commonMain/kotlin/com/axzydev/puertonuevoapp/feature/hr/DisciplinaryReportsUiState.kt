package com.axzydev.puertonuevoapp.feature.hr

import com.axzydev.puertonuevoapp.core.network.users.UserDto

/** Motivos que admite el API (`DisciplinaryReason`), en el orden del formulario. */
val disciplinaryReasonOptions: List<Pair<String, String>> = listOf(
    "ABSENCE" to "Inasistencia",
    "TARDINESS" to "Retardo",
    "INTOXICATION" to "Ebriedad / estado de ebriedad",
    "MISCONDUCT" to "Conducta inapropiada",
    "NONCOMPLIANCE" to "Incumplimiento de funciones",
    "OTHER" to "Otro",
)

fun disciplinaryReasonLabel(reason: String): String =
    disciplinaryReasonOptions.firstOrNull { it.first == reason }?.second ?: reason

/** Estado del formulario de alta de un acta (modal sobre la lista). */
data class DisciplinaryFormState(
    val visible: Boolean = false,
    val personQuery: String = "",
    val people: List<UserDto> = emptyList(),
    val peopleLoading: Boolean = false,
    val selected: UserDto? = null,
    val reason: String = "",
    /** Día local del incidente `YYYY-MM-DD`. */
    val incidentDate: String = "",
    val description: String = "",
    val sanction: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    /** Id del acta recién creada: la pantalla navega a su detalle y lo consume. */
    val createdId: String? = null,
) {
    val canSave: Boolean
        get() = selected != null &&
            reason.isNotBlank() &&
            incidentDate.length == 10 &&
            description.isNotBlank() &&
            !saving
}

/** Estado de la pantalla de actas (el formulario; la tabla vive en el VM paginado). */
data class DisciplinaryReportsUiState(
    val form: DisciplinaryFormState = DisciplinaryFormState(),
)
