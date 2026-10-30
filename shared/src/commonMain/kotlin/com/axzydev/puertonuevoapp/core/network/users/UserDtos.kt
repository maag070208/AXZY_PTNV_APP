package com.axzydev.puertonuevoapp.core.network.users

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import kotlinx.serialization.Serializable

@Serializable
data class SubareaRefDto(
    val id: String,
    val name: String,
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val name: String,
    val role: String,
    val active: Boolean = true,
    val jobTitle: String? = null,
    val area: String? = null,
    val employeeNumber: String? = null,
    val company: String? = null,
    val departmentId: String? = null,
    val department: DepartmentRefDto? = null,
    val subareaId: String? = null,
    val subarea: SubareaRefDto? = null,
)

@Serializable
data class UserCreateInput(
    val username: String,
    val password: String,
    val name: String,
    val role: String,
    val email: String? = null,
    val jobTitle: String? = null,
    val employeeNumber: String? = null,
    val company: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
)

@Serializable
data class UserUpdateInput(
    val username: String? = null,
    val name: String? = null,
    val role: String? = null,
    val email: String? = null,
    val active: Boolean? = null,
    val jobTitle: String? = null,
    val employeeNumber: String? = null,
    val company: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
)

@Serializable
data class UserPasswordInput(val password: String)

/**
 * Entrada del historial de una cuenta (`GET /users/:id/history`): eventos
 * reales de inventario (`LOAN_CUSTODIAN`, `MOVEMENT`), de tickets
 * (`TICKET_CREATED`, `TICKET_ASSIGNED`, `TICKET_COMMENT`) y de la propia cuenta
 * (`USER_DEACTIVATED`, `USER_REACTIVATED`). El API manda [title] y [detail] ya
 * traducidos al español y [refId] apunta al recurso que se puede abrir.
 */
@Serializable
data class UserHistoryEntryDto(
    val id: String,
    /** `LOAN_CUSTODIAN`, `MOVEMENT`, `TICKET_*`, `USER_DEACTIVATED`, `USER_REACTIVATED`… */
    val type: String,
    val title: String,
    val detail: String = "",
    /** ISO-8601 UTC, como el resto de fechas del API. */
    val timestamp: String,
    /** Id del ticket, préstamo o movimiento que originó el evento (si aplica). */
    val refId: String? = null,
)

/** Baja de una cuenta (`PATCH /users/:id/deactivate`): el motivo es obligatorio. */
@Serializable
data class UserDeactivateInput(
    val reason: String,
    /** La API avisa a la persona por defecto (notificación + correo). */
    val notifyUser: Boolean = true,
)

/** Cuerpo vacío de `PATCH /users/:id/reactivate` (la API no pide datos). */
@Serializable
class UserReactivateInput

/**
 * Respuesta de alta/baja de una cuenta: el API devuelve el estado del usuario,
 * no el usuario completo (`UserDeactivateResponseSchema`).
 */
@Serializable
data class UserActiveStateDto(
    val id: String = "",
    val active: Boolean = false,
    val deactivatedAt: String? = null,
    val deactivatedById: String? = null,
    val deactivationReason: String? = null,
)

/** Resultado de `POST /users/import`: creados y filas omitidas con su motivo. */
@Serializable
data class UserImportResultDto(
    val created: Int = 0,
    val skipped: List<UserImportSkippedDto> = emptyList(),
)

/** Una fila del Excel que no se pudo crear (repetida, sin datos o contraseña corta). */
@Serializable
data class UserImportSkippedDto(
    /** Número de fila en Excel (2 = primer renglón de datos). */
    val row: Int = 0,
    val username: String = "",
    val reason: String = "",
)
