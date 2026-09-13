package com.axzydev.puertonuevoapp.feature.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.nav.LocalNavigator
import com.axzydev.puertonuevoapp.core.network.DepartmentDto
import com.axzydev.puertonuevoapp.core.network.UserCreateRequest
import com.axzydev.puertonuevoapp.core.network.UserUpdateRequest
import com.axzydev.puertonuevoapp.core.theme.AppColors
import com.axzydev.puertonuevoapp.core.ui.ErrorState
import com.axzydev.puertonuevoapp.core.ui.LoadingState
import com.axzydev.puertonuevoapp.core.ui.AppSurfaceCard
import com.axzydev.puertonuevoapp.core.ui.SectionLabel
import com.axzydev.puertonuevoapp.core.ui.SimpleDropdownField
import kotlinx.coroutines.launch

private val roleOptions = listOf(
    "ADMIN" to "ADMIN",
    "GERENTE" to "GERENTE",
    "JEFE_DE_AREA" to "JEFE DE AREA",
    "EMPLEADO" to "EMPLEADO",
)

@Composable
fun UserFormScreen(userId: String?) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val isEdit = userId != null
    var loading by remember { mutableStateOf(isEdit) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var departments by remember { mutableStateOf<List<DepartmentDto>>(emptyList()) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("EMPLEADO") }
    var employeeNumber by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var departmentId by remember { mutableStateOf("") }
    var subareaId by remember { mutableStateOf("") }
    var retryKey by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf(0) }

    suspend fun loadData() {
        loading = true
        error = null
        try {
            departments = AppContainer.departmentsApi.list().filter { it.active }
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los departamentos"
        }
        if (userId != null) {
            try {
                val user = AppContainer.usersApi.get(userId)
                username = user.username
                email = user.email.orEmpty()
                name = user.name
                role = user.role
                employeeNumber = user.numeroEmpleado.orEmpty()
                position = user.puesto.orEmpty()
                departmentId = user.departmentId.orEmpty()
                subareaId = user.subareaId.orEmpty()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo cargar el usuario"
            }
        }
        loading = false
    }

    LaunchedEffect(userId, retryKey) { loadData() }

    fun submit() {
        val cleanUsername = username.trim()
        val cleanName = name.trim()
        if (cleanUsername.isBlank() || cleanName.isBlank() || (!isEdit && password.isBlank()) || saving) {
            error = "Completa username, nombre y contraseña"
            return
        }
        scope.launch {
            saving = true
            error = null
            try {
                if (userId == null) {
                    AppContainer.usersApi.create(
                        UserCreateRequest(
                            username = cleanUsername,
                            email = email.trim().ifBlank { null },
                            password = password,
                            name = cleanName,
                            role = role,
                            puesto = position.trim().ifBlank { null },
                            numeroEmpleado = employeeNumber.trim().ifBlank { null },
                            departmentId = departmentId.ifBlank { null },
                            subareaId = subareaId.ifBlank { null },
                        ),
                    )
                } else {
                    AppContainer.usersApi.update(
                        userId,
                        UserUpdateRequest(
                            username = cleanUsername,
                            email = email.trim().ifBlank { null },
                            name = cleanName,
                            role = role,
                            puesto = position.trim().ifBlank { null },
                            numeroEmpleado = employeeNumber.trim().ifBlank { null },
                            departmentId = departmentId.ifBlank { null },
                            subareaId = subareaId.ifBlank { null },
                        ),
                    )
                }
                navigator.pop()
            } catch (e: Exception) {
                error = e.message ?: "No se pudo guardar el usuario"
            } finally {
                saving = false
            }
        }
    }

    when {
            loading -> LoadingState(Modifier.fillMaxSize())
            error != null && username.isBlank() && name.isBlank() -> ErrorState(error ?: "Error", Modifier.fillMaxSize(), onRetry = { retryKey++ })
            else -> Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(16.dp),
            ) {
                Text(if (isEdit) "Editar usuario" else "Nuevo usuario", style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
                Text("Completa la información del usuario paso a paso", style = MaterialTheme.typography.bodySmall, color = AppColors.TextMuted, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(22.dp))
                WizardProgress(step)
                Spacer(Modifier.height(22.dp))
                AppSurfaceCard {
                    when (step) {
                        0 -> {
                            SectionLabel("Datos de acceso")
                            FormField("Correo", email, { email = it }, "usuario@empresa.com")
                            FormField("Username", username, { username = it })
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
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
                            FormField("Nombre completo", name, { name = it })
                            FormField("No. Empleado", employeeNumber, { employeeNumber = it })
                            FormField("Puesto", position, { position = it })
                        }
                        else -> {
                            SectionLabel("Organización y permisos")
                            SimpleDropdownField("Rol", role, roleOptions, { role = it }, Modifier.fillMaxWidth())
                            val department = departments.firstOrNull { it.id == departmentId }
                            SimpleDropdownField(
                                "Departamento",
                                departmentId,
                                listOf("" to "Sin departamento") + departments.map { it.id to it.name },
                                { departmentId = it; subareaId = "" },
                                Modifier.fillMaxWidth().padding(top = 12.dp),
                            )
                            SimpleDropdownField(
                                "Subárea",
                                subareaId,
                                listOf("" to "Sin subárea") + (department?.subareas.orEmpty().filter { it.active }.map { it.id to it.name }),
                                { subareaId = it },
                                Modifier.fillMaxWidth().padding(top = 12.dp),
                            )
                        }
                    }
                }
                if (error != null) Text(error ?: "", color = AppColors.Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step > 0) {
                        androidx.compose.material3.OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).height(52.dp), shape = MaterialTheme.shapes.medium) { Text("Atrás") }
                    }
                    Button(
                        onClick = {
                            val valid = when (step) {
                                0 -> username.trim().isNotBlank() && (isEdit || password.isNotBlank())
                                1 -> name.trim().isNotBlank()
                                else -> true
                            }
                            if (!valid) error = if (step == 0) "Username y contraseña son obligatorios" else "El nombre es obligatorio"
                            else if (step < 2) { error = null; step++ } else submit()
                        },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) { if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp) else Text(if (step == 2) "Guardar usuario" else "Continuar") }
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
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (index <= step) AppColors.EmeraldPrimary else AppColors.SurfaceVariant,
                                androidx.compose.foundation.shape.CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = if (index <= step) AppColors.Surface else AppColors.TextMuted)
                    }
                    if (index < labels.lastIndex) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.weight(1f).height(2.dp).background(if (index < step) AppColors.EmeraldPrimary else AppColors.Outline),
                        )
                    }
                }
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (index == step) AppColors.TextPrimary else AppColors.TextFaint, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
