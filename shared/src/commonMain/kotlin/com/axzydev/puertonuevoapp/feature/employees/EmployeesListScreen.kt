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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.UserDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.EmptyState
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import kotlinx.coroutines.launch

/**
 * Directorio de empleados (rol EMPLEADO), respaldado por el mismo endpoint
 * /users/empleados que ya usa el picker de asignaciones de tickets — visible
 * para cualquier usuario autenticado, igual que en el web. Editar sigue
 * siendo exclusivo de ADMIN y reutiliza la pantalla de edición de Usuarios
 * (misma acción PUT /users/:id que EmployeeFormPage en el web, sin duplicar
 * el formulario).
 */
@Composable
fun EmployeesListScreen() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.role == "ADMIN"

    var employees by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var departmentFilter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            employees = AppContainer.usersApi.empleados().sortedBy { it.name }
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar el directorio de empleados"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val departmentOptions by remember(employees) {
        derivedStateOf { employees.mapNotNull { it.department?.name }.distinct().sorted() }
    }

    val filtered by remember(employees, query, departmentFilter) {
        derivedStateOf {
            employees.filter { e ->
                (departmentFilter.isBlank() || e.department?.name == departmentFilter) &&
                    (query.isBlank() ||
                        e.name.contains(query, ignoreCase = true) ||
                        e.numeroEmpleado.orEmpty().contains(query, ignoreCase = true) ||
                        e.puesto.orEmpty().contains(query, ignoreCase = true))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Directorio de empleados · ${filtered.size} resultados", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted)
            Spacer(Modifier.height(10.dp))
            AppSearchField(query, { query = it }, "Buscar por nombre, puesto o número")
            if (departmentOptions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chips = listOf("" to "Todos") + departmentOptions.map { it to it }
                    chips.forEach { (value, label) ->
                        val selected = departmentFilter == value
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) AppColors.Surface else AppColors.TextMuted,
                            modifier = Modifier
                                .background(if (selected) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, RoundedCornerShape(20.dp))
                                .clickable { departmentFilter = value }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        when {
            loading -> LoadingState(modifier = Modifier.weight(1f))
            error != null -> ErrorState(error ?: "Error", Modifier.weight(1f), onRetry = { scope.launch { load() } })
            filtered.isEmpty() -> EmptyState("No hay empleados con estos filtros", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { e ->
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
                listOfNotNull(
                    employee.puesto,
                    employee.numeroEmpleado?.let { "No. $it" },
                ).joinToString(" · ").ifBlank { "Sin puesto registrado" },
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
