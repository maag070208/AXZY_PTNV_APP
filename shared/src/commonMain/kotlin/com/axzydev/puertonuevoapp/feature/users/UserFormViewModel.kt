package com.axzydev.puertonuevoapp.feature.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UserCreateInput
import com.axzydev.puertonuevoapp.core.network.users.UserUpdateInput
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Alta/edición de usuario en 3 pasos (acceso, perfil, organización). */
class UserFormViewModel(
    private val userId: String?,
    private val usersApi: UsersApi,
    private val departmentsApi: DepartmentsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserFormUiState(loading = true))
    val uiState: StateFlow<UserFormUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = runCatching {
                val departments = departmentsApi.list().filter { it.active }
                val user = userId?.let { usersApi.get(it) }
                departments to user
            }
            result.fold(
                onSuccess = { (departments, user) ->
                    _uiState.update {
                        if (user != null) {
                            it.copy(
                                loading = false,
                                departments = departments,
                                username = user.username,
                                email = user.email.orEmpty(),
                                name = user.name,
                                role = user.role,
                                employeeNumber = user.employeeNumber.orEmpty(),
                                jobTitle = user.jobTitle.orEmpty(),
                                departmentId = user.departmentId.orEmpty(),
                                subareaId = user.subareaId.orEmpty(),
                            )
                        } else {
                            it.copy(loading = false, departments = departments)
                        }
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(loading = false, error = networkMessage(e)) } },
            )
        }
    }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onRoleChange(value: String) = _uiState.update { it.copy(role = value) }
    fun onEmployeeNumberChange(value: String) = _uiState.update { it.copy(employeeNumber = value) }
    fun onJobTitleChange(value: String) = _uiState.update { it.copy(jobTitle = value) }
    fun onDepartmentChange(value: String) = _uiState.update { it.copy(departmentId = value, subareaId = "") }
    fun onSubareaChange(value: String) = _uiState.update { it.copy(subareaId = value) }

    fun goBack() = _uiState.update { if (it.step > 0) it.copy(step = it.step - 1) else it }

    fun goNextOrSubmit() {
        val state = _uiState.value
        val valid = when (state.step) {
            0 -> state.username.trim().isNotBlank() && (userId != null || state.password.isNotBlank())
            1 -> state.name.trim().isNotBlank()
            else -> true
        }
        if (!valid) {
            _uiState.update { it.copy(error = if (state.step == 0) "Username y contraseña son obligatorios" else "El nombre es obligatorio") }
            return
        }
        if (state.step < 2) {
            _uiState.update { it.copy(step = it.step + 1, error = null) }
        } else {
            submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.saving) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                if (userId == null) {
                    usersApi.create(
                        UserCreateInput(
                            username = state.username.trim(),
                            email = state.email.trim().ifBlank { null },
                            password = state.password,
                            name = state.name.trim(),
                            role = state.role,
                            jobTitle = state.jobTitle.trim().ifBlank { null },
                            employeeNumber = state.employeeNumber.trim().ifBlank { null },
                            departmentId = state.departmentId.ifBlank { null },
                            subareaId = state.subareaId.ifBlank { null },
                        ),
                    )
                } else {
                    usersApi.update(
                        userId,
                        UserUpdateInput(
                            username = state.username.trim(),
                            email = state.email.trim().ifBlank { null },
                            name = state.name.trim(),
                            role = state.role,
                            jobTitle = state.jobTitle.trim().ifBlank { null },
                            employeeNumber = state.employeeNumber.trim().ifBlank { null },
                            departmentId = state.departmentId.ifBlank { null },
                            subareaId = state.subareaId.ifBlank { null },
                        ),
                    )
                }
            }
            _uiState.update {
                it.copy(saving = false, error = result.exceptionOrNull()?.let(::networkMessage), saved = result.isSuccess)
            }
        }
    }
}
