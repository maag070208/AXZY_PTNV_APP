package com.axzydev.puertonuevoapp.feature.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec
import com.axzydev.puertonuevoapp.core.util.initials

/**
 * Directorio de personal sobre el componente genérico de tabla
 * (`AppTableScreen` + [PersonalListViewModel]): 10 por página, búsqueda por
 * nombre y filtros por nombre/departamento/estatus/rol. Cada persona es una
 * card elevada con acciones: "Perfil" (expediente de Personal) y "Editar"
 * (exclusivo de ADMIN, reutiliza el formulario de personal).
 */
@Composable
fun EmployeesListScreen(
    viewModel: EmployeesListViewModel = viewModel {
        EmployeesListViewModel(AppContainer.hrApi, AppContainer.departmentsApi)
    },
) {
    val state by viewModel.uiState.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val navigator = LocalNavigator.current
    val authState by AppContainer.authRepository.state.collectAsState()
    val isAdmin = (authState as? AuthState.LoggedIn)?.user?.canManageCatalogs == true

    AppTableScreen(
        state = state,
        heading = "Directorio de personal",
        filterSpecs = employeeFilterSpecs(departments),
        onSearchChange = viewModel::onSearchChange,
        onFiltersChange = viewModel::onFiltersChange,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        searchPlaceholder = "Buscar por nombre",
        emptyMessage = "No hay personal con estos filtros",
        searchKey = "name",
        itemKey = { it.id },
    ) { person ->
        EmployeeCard(
            person = person,
            isAdmin = isAdmin,
            onOpenProfile = { navigator.push(Screen.EmployeeProfile(person.id)) },
            onEdit = { navigator.push(Screen.UserForm(person.id)) },
        )
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

/** Card elevada de personal: avatar con gradiente por rol, chips y acciones pill (Perfil / Editar). */
@Composable
private fun EmployeeCard(
    person: UserDto,
    isAdmin: Boolean,
    onOpenProfile: () -> Unit,
    onEdit: () -> Unit,
) {
    val accent = AppColors.roleAccent(person.role)
    val gradient = Brush.verticalGradient(listOf(accent, lerp(accent, Color.Black, 0.30f)))

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenProfile,
        shape = AppShape.card,
        elevation = 2.dp,
        borderColor = AppColors.Outline.copy(alpha = 0.5f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(gradient, AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(person.name),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
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
                    "@${person.username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        person.jobTitle?.takeIf { it.isNotBlank() },
                        person.department?.name,
                    ).joinToString(" · ").ifBlank { "Sin puesto registrado" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusChip(
                label = if (person.active) "Activo" else "Inactivo",
                color = if (person.active) AppColors.Success else AppColors.Danger,
            )
        }

        Spacer(Modifier.size(12.dp))
        HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.35f))
        Spacer(Modifier.size(10.dp))

        Row {
            CardActionPill(
                label = "Perfil",
                icon = Icons.Filled.Person,
                color = AppColors.Info,
                onClick = onOpenProfile,
            )
            if (isAdmin) {
                Spacer(Modifier.width(8.dp))
                CardActionPill(
                    label = "Editar",
                    icon = Icons.Filled.Edit,
                    color = AppColors.EmeraldPrimary,
                    onClick = onEdit,
                )
            }
        }
    }
}

/** Botón pill pequeño de acciones dentro de una card. Recorta ripple al pill con [AppShape.pill]. */
@Composable
private fun CardActionPill(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(AppShape.pill)
            .background(color.copy(alpha = 0.12f), AppShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}