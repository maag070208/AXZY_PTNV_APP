package com.axzydev.puertonuevoapp.feature.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.axzydev.puertonuevoapp.core.session.PermissionKeys
import com.axzydev.puertonuevoapp.core.theme.AppBento
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.theme.AppShape
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppCard
import com.axzydev.puertonuevoapp.core.ui.AppModal
import com.axzydev.puertonuevoapp.core.ui.AppModalTone
import com.axzydev.puertonuevoapp.core.ui.AppPasswordField
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoMiniStat
import com.axzydev.puertonuevoapp.core.ui.BentoState
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.PillButtonVariant
import com.axzydev.puertonuevoapp.core.ui.StatusChip
import com.axzydev.puertonuevoapp.core.ui.table.AppTableScreen
import com.axzydev.puertonuevoapp.core.ui.table.FilterControl
import com.axzydev.puertonuevoapp.core.ui.table.FilterOption
import com.axzydev.puertonuevoapp.core.ui.table.TableFilterSpec
import com.axzydev.puertonuevoapp.core.util.initials
import com.axzydev.puertonuevoapp.core.util.roleLabel

/**
 * Lista de cuentas de usuario (`/users` de la web): tabla server-side
 * (`AppTableScreen` sobre `POST /users/query`), tira de totales reales,
 * alta e importación (con `users.create`) y acciones por fila: historial,
 * editar, cambiar contraseña y activar/desactivar (con `users.edit`).
 *
 * La edición reutiliza `Screen.UserForm`; el historial abre
 * `Screen.UserHistory`. La app solo oculta: el API vuelve a validar el permiso
 * de cada acción.
 */
@Composable
fun UsersListScreen(
    viewModel: UsersListViewModel = viewModel {
        UsersListViewModel(AppContainer.usersApi, AppContainer.departmentsApi)
    },
) {
    val table by viewModel.uiState.collectAsState()
    val screen by viewModel.screen.collectAsState()
    val authState by AppContainer.authRepository.state.collectAsState()
    val user = (authState as? AuthState.LoggedIn)?.user
    val navigator = LocalNavigator.current

    if (user == null || !user.can(PermissionKeys.USERS_VIEW)) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            BentoState(
                message = "No autorizado — se requiere el permiso Usuarios",
                tone = BentoTone.Rose,
                icon = Icons.Filled.Lock,
            )
        }
        return
    }

    val canCreate = user.can(PermissionKeys.USERS_CREATE)
    val canEdit = user.can(PermissionKeys.USERS_EDIT)

    // Al volver de editar una cuenta o de la importación, el ViewModel sigue
    // vivo: si ya había filas se re-pide la página y los totales. En la primera
    // entrada la carga inicial está en curso y no hace falta duplicarla.
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.items.isNotEmpty()) {
            viewModel.refresh()
            viewModel.loadCounts()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        UsersHeader(
            screen = screen,
            canCreate = canCreate,
            onNew = { navigator.push(Screen.UserForm()) },
            onImport = { navigator.push(Screen.UserImport) },
        )

        AppTableScreen(
            state = table,
            heading = "Cuentas de usuario",
            filterSpecs = userFilterSpecs(screen.departments),
            onSearchChange = viewModel::onSearchChange,
            onFiltersChange = viewModel::onFiltersChange,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            searchPlaceholder = "Buscar por nombre",
            emptyMessage = "No hay cuentas con estos filtros",
            searchKey = "name",
            itemKey = { it.id },
            modifier = Modifier.weight(1f),
        ) { account ->
            UserAccountCard(
                account = account,
                canEdit = canEdit,
                onOpen = { navigator.push(Screen.UserHistory(account.id)) },
                onEdit = { navigator.push(Screen.UserForm(account.id)) },
                onPassword = { viewModel.requestPassword(account) },
                onToggleActive = {
                    if (account.active) viewModel.requestDeactivate(account) else viewModel.requestReactivate(account)
                },
            )
        }
    }

    // ── Diálogos ─────────────────────────────────────────────────────────────

    screen.deactivateTarget?.let { target ->
        AppModal(
            title = "Dar de baja la cuenta",
            icon = Icons.Filled.PersonOff,
            tone = AppModalTone.Danger,
            onDismiss = viewModel::dismissDeactivate,
            confirmLabel = "Dar de baja",
            onConfirm = viewModel::confirmDeactivate,
            confirmEnabled = screen.canConfirmDeactivate,
            saving = screen.saving,
        ) {
            Text(
                "La cuenta @${target.username} dejará de poder entrar, pero conserva su expediente e historial.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(14.dp))
            AppTextField(
                value = screen.deactivateReason,
                onValueChange = viewModel::onDeactivateReasonChange,
                label = { Text("Motivo de la baja") },
                placeholder = { Text("Ej. renuncia, cambio de puesto…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Se enviará un aviso a la persona.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
            )
            screen.error?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }

    screen.reactivateTarget?.let { target ->
        AppModal(
            title = "Reactivar la cuenta",
            icon = Icons.Filled.RestartAlt,
            tone = AppModalTone.Success,
            onDismiss = viewModel::dismissReactivate,
            confirmLabel = "Reactivar",
            onConfirm = viewModel::confirmReactivate,
            saving = screen.saving,
        ) {
            Text(
                "La cuenta @${target.username} volverá a poder entrar al sistema.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            screen.error?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }

    screen.passwordTarget?.let { target ->
        AppModal(
            title = "Cambiar contraseña",
            icon = Icons.Filled.Key,
            tone = AppModalTone.Success,
            onDismiss = viewModel::dismissPassword,
            confirmLabel = "Cambiar contraseña",
            onConfirm = viewModel::confirmPassword,
            confirmEnabled = screen.canConfirmPassword,
            saving = screen.saving,
        ) {
            Text(
                "Nueva contraseña para @${target.username}. La anterior deja de funcionar de inmediato.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(14.dp))
            AppPasswordField(
                value = screen.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = { Text("Contraseña nueva") },
                placeholder = { Text("Mínimo 6 caracteres") },
                modifier = Modifier.fillMaxWidth(),
            )
            screen.error?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
}

/** Tira de totales y accesos de alta/importación, sobre la tabla. */
@Composable
private fun UsersHeader(
    screen: UsersListUiState,
    canCreate: Boolean,
    onNew: () -> Unit,
    onImport: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 14.dp)) {
        screen.counts?.let { counts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BentoMiniStat(
                    label = "Cuentas",
                    value = counts.total.toString(),
                    tone = BentoTone.Sea,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Activas",
                    value = counts.active.toString(),
                    tone = BentoTone.Mint,
                    modifier = Modifier.weight(1f),
                )
                BentoMiniStat(
                    label = "Inactivas",
                    value = counts.inactive.toString(),
                    tone = if (counts.inactive > 0) BentoTone.Sun else BentoTone.Plain,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        if (canCreate) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    text = "Importar",
                    onClick = onImport,
                    variant = PillButtonVariant.Secondary,
                    leadingIcon = Icons.Filled.UploadFile,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Nueva cuenta",
                    onClick = onNew,
                    leadingIcon = Icons.Filled.PersonAdd,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** Filtros por columna de la tabla (el buscador cubre `name`). */
private fun userFilterSpecs(departments: List<DepartmentDto>): List<TableFilterSpec> = listOf(
    TableFilterSpec("name", "Nombre", FilterControl.Text),
    TableFilterSpec("username", "Usuario", FilterControl.Text),
    TableFilterSpec(
        key = "role",
        label = "Rol",
        control = FilterControl.Select(userRoleFilterOptions.map { (value, label) -> FilterOption(value, label) }),
    ),
    TableFilterSpec(
        key = "departmentId",
        label = "Departamento",
        control = FilterControl.Select(departments.map { FilterOption(it.id, it.name) }),
    ),
    TableFilterSpec(
        key = "active",
        label = "Estatus",
        control = FilterControl.Select(
            listOf(
                FilterOption("true", "Activas"),
                FilterOption("false", "Inactivas"),
            ),
        ),
    ),
)

/** Tarjeta de una cuenta: identidad, rol, estado y acciones por permiso. */
@Composable
private fun UserAccountCard(
    account: UserDto,
    canEdit: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onPassword: () -> Unit,
    onToggleActive: () -> Unit,
) {
    val accent = AppColors.roleAccent(account.role)
    val gradient = Brush.verticalGradient(listOf(accent, lerp(accent, Color.Black, 0.30f)))
    val plain = AppBento.tone(BentoTone.Plain)

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        shape = AppShape.card,
        elevation = 2.dp,
        borderColor = AppColors.Outline.copy(alpha = 0.5f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(gradient, AppShape.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(account.name),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "@${account.username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        account.department?.name,
                        account.subarea?.name?.takeIf { it.isNotBlank() },
                        account.jobTitle?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "Sin departamento" },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(label = roleLabel(account.role), tone = roleTone(account.role))
                Spacer(Modifier.height(6.dp))
                StatusChip(
                    label = if (account.active) "Activa" else "Inactiva",
                    tone = if (account.active) BentoTone.Mint else BentoTone.Rose,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = plain.content.copy(alpha = 0.12f))
        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                account.employeeNumber?.takeIf { it.isNotBlank() }?.let { "No. empleado $it" } ?: "Sin número de empleado",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextFaint,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpen) {
                Icon(Icons.Filled.History, contentDescription = "Historial de la cuenta", tint = AppColors.Info)
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar cuenta", tint = AppColors.EmeraldPrimary)
                }
                IconButton(onClick = onPassword) {
                    Icon(Icons.Filled.Key, contentDescription = "Cambiar contraseña", tint = AppColors.EmeraldPrimary)
                }
                IconButton(onClick = onToggleActive) {
                    Icon(
                        imageVector = if (account.active) Icons.Filled.PersonOff else Icons.Filled.RestartAlt,
                        contentDescription = if (account.active) "Dar de baja" else "Reactivar",
                        tint = if (account.active) AppColors.Danger else AppColors.Success,
                    )
                }
            }
        }
    }
}

