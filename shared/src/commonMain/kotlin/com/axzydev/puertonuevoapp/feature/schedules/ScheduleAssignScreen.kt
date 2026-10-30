package com.axzydev.puertonuevoapp.feature.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.session.AuthState
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppSearchField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import androidx.compose.ui.text.style.TextOverflow

/**
 * Asignación masiva de horarios (`schedules.manage`).
 *
 * Se elige el horario, se filtra por departamento o se busca a la persona, y se
 * marcan varias para **asignarlas desde una fecha** o **quitarles la vigencia**
 * de un jalón. Sin arrastrar y soltar: dos listas y dos botones, como la web.
 */
@Composable
fun ScheduleAssignScreen() {
    val authState by AppContainer.authRepository.state.collectAsState()
    val canManage = (authState as? AuthState.LoggedIn)?.user?.can(PermissionKeys.SCHEDULES_MANAGE) == true

    if (!canManage) {
        BentoState(
            message = "No autorizado — requiere el permiso Administrar horarios",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
        )
        return
    }

    val viewModel: ScheduleAssignViewModel = viewModel(key = "schedule-assign") {
        ScheduleAssignViewModel(SchedulesServices.api, AppContainer.usersApi, AppContainer.departmentsApi)
    }
    val state by viewModel.uiState.collectAsState()

    when {
        state.loading -> LoadingState(modifier = Modifier.fillMaxSize())

        state.error != null -> BentoState(
            message = state.error ?: "No se pudo cargar la asignación",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            tone = BentoTone.Rose,
            actionLabel = "Reintentar",
            onAction = viewModel::retry,
        )

        state.schedules.isEmpty() -> BentoState(
            message = "No hay horarios activos. Crea uno primero en Horarios.",
            modifier = Modifier.fillMaxSize().padding(16.dp),
            actionLabel = "Reintentar",
            onAction = viewModel::retry,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        "Asignar horarios",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        "Un horario para varias personas a la vez, con vigencia desde una fecha.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                    )
                }
            }

            item {
                SimpleDropdownField(
                    label = "Horario",
                    value = state.scheduleId,
                    options = state.schedules.map { it.id to it.name },
                    onSelect = viewModel::onScheduleChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                SimpleDropdownField(
                    label = "Departamento",
                    value = state.departmentId,
                    options = buildList {
                        add("" to "Todos los departamentos")
                        state.departments.forEach { add(it.id to it.name) }
                    },
                    onSelect = viewModel::onDepartmentChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = state.fromDate,
                        onValueChange = viewModel::onFromDateChange,
                        label = { Text("Desde") },
                        placeholder = { Text("AAAA-MM-DD") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Cierra la vigencia anterior de cada persona y arranca la nueva ese día.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                        modifier = Modifier.weight(1f).padding(top = 22.dp),
                    )
                }
            }

            item {
                AppSearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = "Buscar por nombre o número…",
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    BentoMiniStat(
                        label = "En el horario",
                        value = state.assigned.size.toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Mint,
                    )
                    BentoMiniStat(
                        label = "Por asignar",
                        value = state.available.size.toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Sun,
                    )
                    BentoMiniStat(
                        label = "Personal",
                        value = state.employees.size.toString(),
                        modifier = Modifier.weight(1f),
                        tone = BentoTone.Sea,
                    )
                }
            }

            state.validationError?.let { message ->
                item {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
                }
            }

            // ── Personas sin el horario ─────────────────────────────────────
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(
                        "Sin el horario · ${state.available.size}",
                        modifier = Modifier.weight(1f),
                    )
                    if (state.selectedToAssign.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearAssignSelection) {
                            Text("Limpiar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TextButton(onClick = viewModel::selectAllAvailable) {
                        Text("Todos", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (state.available.isEmpty()) {
                item {
                    BentoState(
                        message = if (state.query.isBlank() && state.departmentId.isBlank()) {
                            "Todo el personal ya tiene este horario."
                        } else {
                            "Nadie coincide con el filtro."
                        },
                    )
                }
            } else {
                items(state.available.take(80), key = { "available-${it.id}" }) { person ->
                    AssignPersonRow(
                        person = person,
                        selected = person.id in state.selectedToAssign,
                        trailing = Icons.Filled.Add,
                        onClick = { viewModel.toggleAssign(person.id) },
                    )
                }
                if (state.available.size > 80) {
                    item {
                        Text(
                            "Mostrando 80 de ${state.available.size}. Filtra por departamento o busca por nombre.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                    }
                }
            }

            item {
                PillButton(
                    text = "Asignar ${state.selectedToAssign.size} persona(s)",
                    onClick = viewModel::assign,
                    variant = PillButtonVariant.Primary,
                    enabled = state.canAssign,
                    loading = state.saving,
                    leadingIcon = Icons.Filled.CheckCircle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Personas con el horario ─────────────────────────────────────
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(
                        "Con el horario · ${state.assigned.size}",
                        modifier = Modifier.weight(1f),
                    )
                    if (state.selectedToRemove.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearRemoveSelection) {
                            Text("Limpiar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TextButton(onClick = viewModel::selectAllAssigned) {
                        Text("Todos", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (state.assignedLoading) {
                item {
                    BentoState("Cargando asignaciones…", loading = true)
                }
            } else if (state.assignedPeople.isEmpty()) {
                item {
                    BentoState("Nadie tiene este horario todavía.")
                }
            } else {
                items(state.assignedPeople.take(80), key = { "assigned-${it.id}" }) { person ->
                    AssignPersonRow(
                        person = person,
                        selected = person.id in state.selectedToRemove,
                        trailing = Icons.Filled.DeleteOutline,
                        onClick = { viewModel.toggleRemove(person.id) },
                    )
                }
                if (state.assignedPeople.size > 80) {
                    item {
                        Text(
                            "Mostrando 80 de ${state.assignedPeople.size}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextFaint,
                        )
                    }
                }
                item {
                    PillButton(
                        text = "Quitar ${state.selectedToRemove.size} asignación(es)",
                        onClick = viewModel::remove,
                        variant = PillButtonVariant.Secondary,
                        enabled = state.canRemove,
                        loading = state.saving,
                        leadingIcon = Icons.Filled.Groups,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AssignPersonRow(
    person: AssignPerson,
    selected: Boolean,
    trailing: ImageVector,
    onClick: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = if (selected) AppColors.EmeraldPrimary else AppColors.Outline.copy(alpha = 0.45f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    person.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (person.meta.isNotBlank()) {
                    Text(
                        person.meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                if (selected) Icons.Filled.CheckCircle else trailing,
                contentDescription = null,
                tint = if (selected) AppColors.EmeraldPrimary else AppColors.TextFaint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
