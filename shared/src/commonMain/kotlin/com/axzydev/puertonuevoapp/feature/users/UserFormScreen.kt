package com.axzydev.puertonuevoapp.feature.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField

@Composable
fun UserFormScreen(userId: String?) {
    val navigator = LocalNavigator.current
    val viewModel: UserFormViewModel = viewModel(key = "user-form-${userId ?: "new"}") {
        UserFormViewModel(userId, AppContainer.usersApi, AppContainer.departmentsApi)
    }
    val state by viewModel.uiState.collectAsState()
    val isEdit = userId != null

    LaunchedEffect(state.saved) { if (state.saved) navigator.pop() }

    if (state.loading) {
        LoadingState(Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
    ) {
        Text(if (isEdit) "Editar usuario" else "Nuevo usuario", style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
        Text("Completa la información del usuario paso a paso", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(22.dp))
        WizardProgress(state.step)
        Spacer(Modifier.height(22.dp))
        AppSurfaceCard {
            when (state.step) {
                0 -> {
                    SectionLabel("Datos de acceso")
                    FormField("Correo", state.email, viewModel::onEmailChange, "usuario@empresa.com")
                    FormField("Username", state.username, viewModel::onUsernameChange)
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text(if (isEdit) "Contraseña (opcional)" else "Contraseña") },
                        placeholder = if (isEdit) ({ Text("Dejar en blanco para no cambiar") }) else null,
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
                1 -> {
                    SectionLabel("Información personal")
                    FormField("Nombre completo", state.name, viewModel::onNameChange)
                    FormField("No. Empleado", state.numeroEmpleado, viewModel::onNumeroEmpleadoChange)
                    FormField("Puesto", state.puesto, viewModel::onPuestoChange)
                }
                else -> {
                    SectionLabel("Organización y permisos")
                    SimpleDropdownField("Rol", state.role, userRoleOptions, viewModel::onRoleChange, Modifier.fillMaxWidth())
                    SimpleDropdownField(
                        "Departamento",
                        state.departmentId,
                        listOf("" to "Sin departamento") + state.departments.map { it.id to it.name },
                        viewModel::onDepartmentChange,
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    SimpleDropdownField(
                        "Subárea",
                        state.subareaId,
                        listOf("" to "Sin subárea") + (state.selectedDepartment?.subareas.orEmpty().filter { it.active }.map { it.id to it.name }),
                        viewModel::onSubareaChange,
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            }
        }
        state.error?.let { Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.step > 0) {
                OutlinedButton(onClick = viewModel::goBack, modifier = Modifier.weight(1f).height(52.dp), shape = MaterialTheme.shapes.medium) { Text("Atrás") }
            }
            Button(
                onClick = viewModel::goNextOrSubmit,
                enabled = !state.saving,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
                else Text(if (state.step == 2) "Guardar usuario" else "Continuar")
            }
        }
    }
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun WizardProgress(step: Int) {
    val labels = listOf("Acceso", "Perfil", "Organización")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).background(if (index <= step) AppColors.EmeraldPrimary else AppColors.SurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = if (index <= step) AppColors.Surface else AppColors.TextMuted)
                    }
                    if (index < labels.lastIndex) {
                        Box(modifier = Modifier.weight(1f).height(2.dp).background(if (index < step) AppColors.EmeraldPrimary else AppColors.Outline))
                    }
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (index == step) AppColors.TextPrimary else AppColors.TextFaint, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
