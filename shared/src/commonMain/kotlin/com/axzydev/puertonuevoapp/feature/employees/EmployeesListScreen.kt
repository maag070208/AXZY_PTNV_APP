package com.axzydev.puertonuevoapp.feature.employees

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState

/**
 * Directorio de empleados, respaldado por el mismo endpoint /users/empleados
 * que usa el picker de asignaciones de tickets. Editar sigue siendo
 * exclusivo de ADMIN y reutiliza la misma pantalla de edición de Usuarios.
 */
@Composable
fun EmployeesListScreen(viewModel: EmployeesListViewModel = viewModel { EmployeesListViewModel(AppContainer.usersApi) }) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.canManageCatalogs == true

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Directorio de empleados · ${state.filtered.size} resultados", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            AppSearchField(state.query, viewModel::onQueryChange, "Buscar por nombre, puesto o número")
            if (state.departmentOptions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf("" to "Todos") + state.departmentOptions.map { it to it }
                    chips.forEach { (value, label) ->
                        val selected = state.departmentFilter == value
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) AppColors.Surface else AppColors.TextMuted,
                            modifier = Modifier
                                .background(if (selected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable { viewModel.onDepartmentFilterChange(value) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        when {
            state.loading -> LoadingState(modifier = Modifier.weight(1f))
            state.error != null -> ErrorState(state.error ?: "Error", Modifier.weight(1f), onRetry = viewModel::load)
            state.filtered.isEmpty() -> EmptyState("No hay empleados con estos filtros", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { it.id }) { e ->
                    EmployeeCard(
                        employee = e,
                        editable = isAdmin,
                        onClick = { if (isAdmin) navigator.push(Screen.UserForm(e.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployeeCard(employee: UserDto, editable: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
            .let { if (editable) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(employee.name, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Text(
                listOfNotNull(employee.puesto, employee.numeroEmpleado?.let { "No. $it" }).joinToString(" · ").ifBlank { "Sin puesto registrado" },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                maxLines = 1,
            )
            val deptLine = listOfNotNull(employee.department?.name, employee.subarea?.name).joinToString(" · ")
            if (deptLine.isNotBlank()) {
                Text(deptLine, style = MaterialTheme.typography.bodySmall, color = AppColors.TextFaint, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (editable) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Editar empleado", tint = AppColors.TextFaint, modifier = Modifier.size(22.dp))
        }
    }
}
