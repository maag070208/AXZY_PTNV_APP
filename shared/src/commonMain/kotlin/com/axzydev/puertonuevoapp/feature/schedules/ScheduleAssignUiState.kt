package com.axzydev.puertonuevoapp.feature.schedules

import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleAssigneeDto
import com.axzydev.puertonuevoapp.core.network.schedules.ScheduleDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto

/** Persona mostrada en las dos listas de la asignación. */
data class AssignPerson(
    val id: String,
    val name: String,
    val employeeNumber: String? = null,
    val jobTitle: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
) {
    /** "#número · puesto · departamento": lo justo para reconocer a la persona. */
    val meta: String
        get() = listOfNotNull(
            employeeNumber?.takeIf { it.isNotBlank() }?.let { "#$it" },
            jobTitle?.takeIf { it.isNotBlank() },
            departmentName?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
}

/**
 * Estado de la asignación masiva: un horario → N personas desde una fecha, y la
 * quita de asignaciones vigentes. Las dos listas (`available` sin el horario y
 * `assigned` con él) filtran por departamento y búsqueda en cliente, como la web.
 */
data class ScheduleAssignUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val schedules: List<ScheduleDto> = emptyList(),
    val employees: List<UserDto> = emptyList(),
    val departments: List<DepartmentDto> = emptyList(),
    val scheduleId: String = "",
    val departmentId: String = "",
    val query: String = "",
    val fromDate: String = todayDateKey(),
    val assigned: List<ScheduleAssigneeDto> = emptyList(),
    val assignedLoading: Boolean = false,
    val selectedToAssign: Set<String> = emptySet(),
    val selectedToRemove: Set<String> = emptySet(),
    val saving: Boolean = false,
    val validationError: String? = null,
) {
    val currentSchedule: ScheduleDto? get() = schedules.firstOrNull { it.id == scheduleId }

    private val assignedIds: Set<String> get() = assigned.mapTo(mutableSetOf()) { it.userId }

    private val employeeById: Map<String, UserDto> get() = employees.associateBy { it.id }

    private fun matches(person: AssignPerson): Boolean {
        val term = query.trim().lowercase()
        if (term.isEmpty()) return true
        return person.name.lowercase().contains(term) ||
            (person.employeeNumber ?: "").lowercase().contains(term)
    }

    private fun matchesDepartment(departmentId: String?): Boolean =
        this.departmentId.isBlank() || departmentId == this.departmentId

    /** Personas que todavía no tienen este horario. */
    val available: List<AssignPerson>
        get() = employees
            .filter { it.id !in assignedIds }
            .map { user ->
                AssignPerson(
                    id = user.id,
                    name = user.name,
                    employeeNumber = user.employeeNumber,
                    jobTitle = user.jobTitle,
                    departmentId = user.departmentId,
                    departmentName = user.department?.name,
                )
            }
            .filter { matchesDepartment(it.departmentId) && matches(it) }
            .sortedBy { it.name.lowercase() }

    /** Personas con asignación vigente de este horario. */
    val assignedPeople: List<AssignPerson>
        get() = assigned.map { row ->
            val user = employeeById[row.userId]
            AssignPerson(
                id = row.userId,
                name = user?.name ?: row.employeeName,
                employeeNumber = user?.employeeNumber ?: row.employeeNumber,
                jobTitle = user?.jobTitle,
                departmentId = user?.departmentId,
                departmentName = user?.department?.name,
            )
        }
            .filter { matchesDepartment(it.departmentId) && matches(it) }
            .sortedBy { it.name.lowercase() }

    val canAssign: Boolean get() = scheduleId.isNotBlank() && selectedToAssign.isNotEmpty() && !saving

    val canRemove: Boolean get() = scheduleId.isNotBlank() && selectedToRemove.isNotEmpty() && !saving
}
