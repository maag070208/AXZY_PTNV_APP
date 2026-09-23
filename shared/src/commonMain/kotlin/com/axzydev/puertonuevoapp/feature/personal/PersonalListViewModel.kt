package com.axzydev.puertonuevoapp.feature.personal

import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.http.TableRequest
import com.axzydev.puertonuevoapp.core.network.http.TableResponse
import com.axzydev.puertonuevoapp.core.network.personal.PersonalApi
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.ui.table.PaginatedTableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tabla de personal (RH), server-side vía `POST /personal/query`
 * (filtros `name`, `departmentId`, `active`, `role`), 10 por página.
 * El catálogo de departamentos alimenta el filtro del modal.
 */
class PersonalListViewModel(
    private val personalApi: PersonalApi,
    private val departmentsApi: DepartmentsApi,
) : PaginatedTableViewModel<UserDto>() {

    override val pageSize: Int = 10
    override val searchKey: String? = "name"

    private val _departments = MutableStateFlow<List<DepartmentDto>>(emptyList())
    val departments: StateFlow<List<DepartmentDto>> = _departments.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { departmentsApi.list() }
                .onSuccess { list -> _departments.value = list.filter { it.active }.sortedBy { it.name } }
        }
    }

    override suspend fun fetch(page: Int, limit: Int, filters: Map<String, String>): TableResponse<UserDto> =
        personalApi.query(TableRequest(page = page, limit = limit, filters = filters))
}