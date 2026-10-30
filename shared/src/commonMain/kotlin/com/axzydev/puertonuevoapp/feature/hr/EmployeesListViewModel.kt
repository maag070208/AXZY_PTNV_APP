package com.axzydev.puertonuevoapp.feature.hr

import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import com.axzydev.puertonuevoapp.core.network.hr.PersonalStatsDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tabla de personal (RH), server-side vía `POST /hr/query`
 * (filtros `name`, `departmentId`, `active`, `role`), 10 por página.
 * El catálogo de departamentos alimenta el filtro del modal.
 */
class EmployeesListViewModel(
    private val hrApi: HrApi,
    private val departmentsApi: DepartmentsApi,
) : PaginatedTableViewModel<UserDto>() {

    override val pageSize: Int = 10
    override val searchKey: String? = "name"

    private val _departments = MutableStateFlow<List<DepartmentDto>>(emptyList())
    val departments: StateFlow<List<DepartmentDto>> = _departments.asStateFlow()

    /** KPIs de la plantilla (`GET /hr/stats`): total, activos e inactivos. */
    private val _stats = MutableStateFlow<PersonalStatsDto?>(null)
    val stats: StateFlow<PersonalStatsDto?> = _stats.asStateFlow()

    init {
        loadDepartments()
        loadStats()
    }

    private fun loadDepartments() {
        viewModelScope.launch {
            runCatching { departmentsApi.list() }
                .onSuccess { list -> _departments.value = list.filter { it.active }.sortedBy { it.name } }
        }
    }

    /** Se reintenta al refrescar la tabla (por si el fallo fue de red). */
    private fun loadStats() {
        viewModelScope.launch {
            runCatching { hrApi.stats() }.onSuccess { _stats.value = it }
        }
    }

    /** El pull-to-refresh de la tabla no toca los KPIs: la pantalla llama a esto. */
    fun reloadStats() = loadStats()

    override suspend fun fetch(page: Int, limit: Int, filters: Map<String, String>): TableResponse<UserDto> =
        hrApi.query(TableRequest(page = page, limit = limit, filters = filters))
}