package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.nav.Screen
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.users.UserDto
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.BentoAvatar
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec

/**
 * Directorio de personal sobre el componente genérico de tabla
 * (`AppTableScreen` + [EmployeesListViewModel]): 10 por página, búsqueda por
 * nombre y filtros por nombre/departamento/estatus/rol.
 *
 * Presentación: franja de 3 indicadores compactos (plantilla, activos,
 * inactivos) y una fila por persona con avatar, nombre, número de empleado,
 * puesto/área, chip de estado y acciones «Perfil» / «Editar» como píldoras.
 */
@Composable
fun EmployeesListScreen(
    viewModel: EmployeesListViewModel = viewModel {
        EmployeesListViewModel(AppContainer.hrApi, AppContainer.departmentsApi)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val canEdit = (authState as? AuthState.LoggedIn)?.user?.canEditUsers == true

    Column(modifier = Modifier.fillMaxSize().background(AppBento.canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BentoMiniStat(
                label = "Personal",
                value = (stats?.total ?: state.total).toString(),
                tone = BentoTone.Sea,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Activos",
                value = stats?.active?.toString() ?: "—",
                tone = if ((stats?.active ?: 0) == 0) BentoTone.Plain else BentoTone.Mint,
                modifier = Modifier.weight(1f),
            )
            BentoMiniStat(
                label = "Inactivos",
                value = stats?.inactive?.toString() ?: "—",
                tone = if ((stats?.inactive ?: 0) == 0) BentoTone.Plain else BentoTone.Rose,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))

        AppTableScreen(
            state = state,
            heading = "Directorio de personal",
            filterSpecs = employeeFilterSpecs(departments),
            onSearchChange = viewModel::onSearchChange,
            onFiltersChange = viewModel::onFiltersChange,
            onRefresh = {
                viewModel.refresh()
                viewModel.reloadStats()
            },
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            searchPlaceholder = "Buscar por nombre",
            emptyMessage = "No hay personal con estos filtros",
            searchKey = "name",
            itemKey = { it.id },
            modifier = Modifier.weight(1f),
        ) { person ->
            EmployeeCard(
                person = person,
                canEdit = canEdit,
                onOpenProfile = { navigator.push(Screen.EmployeeProfile(person.id)) },
                onEdit = { navigator.push(Screen.UserForm(person.id)) },
            )
        }
    }
}

private fun employeeFilterSpecs(departments: List<DepartmentDto>): List<TableFilterSpec> = listOf(
    TableFilterSpec("name", "Nombre", FilterControl.Text),
    TableFilterSpec(
        "departmentId",
        "Departamento",
        FilterControl.Select(departments.map { FilterOption(it.id, it.name) }),
    ),
    TableFilterSpec(
        "active",
        "Estatus",
        FilterControl.Select(
            listOf(
                FilterOption("true", "Activos"),
                FilterOption("false", "Inactivos"),
            ),
        ),
    ),
    TableFilterSpec(
        "role",
        "Rol",
        FilterControl.Select(
            listOf(
                FilterOption("MANAGER", "Gerente"),
                FilterOption("AREA_HEAD", "Jefe de área"),
                FilterOption("EMPLOYEE", "Empleado"),
            ),
        ),
    ),
)

/**
 * Fila de personal: avatar por rol, nombre (título), número de empleado y
 * puesto/área (meta), chip Activo/Inactivo y acciones como píldoras.
 */
@Composable
private fun EmployeeCard(
    person: UserDto,
    canEdit: Boolean,
    onOpenProfile: () -> Unit,
    onEdit: () -> Unit,
) {
    val accent = AppColors.roleAccent(person.role)
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenProfile,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(name = person.name, color = accent, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        person.employeeNumber?.takeIf { it.isNotBlank() }?.let { "#$it" },
                        person.jobTitle?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Sin puesto registrado" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        person.department?.name?.takeIf { it.isNotBlank() },
                        person.subarea?.name?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { person.username },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(
                label = if (person.active) "Activo" else "Inactivo",
                tone = if (person.active) BentoTone.Mint else BentoTone.Rose,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Perfil",
                onClick = onOpenProfile,
                variant = PillButtonVariant.Secondary,
                leadingIcon = Icons.Filled.Person,
                modifier = Modifier.weight(1f),
            )
            if (canEdit) {
                PillButton(
                    text = "Editar",
                    onClick = onEdit,
                    variant = PillButtonVariant.Ghost,
                    leadingIcon = Icons.Filled.Edit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
