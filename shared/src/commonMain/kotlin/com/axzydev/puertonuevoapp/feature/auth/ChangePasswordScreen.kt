package com.axzydev.puertonuevoapp.feature.auth

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.theme.BentoTone
import com.axzydev.puertonuevoapp.core.ui.AppTextField
import com.axzydev.puertonuevoapp.core.ui.BentoSheet
import com.axzydev.puertonuevoapp.core.ui.BrandGradientBackdrop
import com.axzydev.puertonuevoapp.core.ui.BrandLogoBadge
import com.axzydev.puertonuevoapp.core.ui.OnBrandChip
import com.axzydev.puertonuevoapp.core.ui.PillButton
import com.axzydev.puertonuevoapp.core.ui.StatusChip

/**
 * Compuerta de **contraseña provisional** (paridad con `ForcePasswordChange`
 * de la web): mientras `SessionUser.mustChangePassword` sea true la app no
 * debe mostrar el shell. El host la monta en lugar del shell y la retira
 * cuando [onDone] se invoca o cuando el estado de la sesión deja de estar
 * marcado.
 *
 * Contrato público:
 * - [onDone]: se llama **después** de que el API aceptó el cambio y el
 *   repositorio ya apagó `mustChangePassword` en el estado de sesión (el
 *   shell aparecería solo); sirve para navegar o cerrar un host tipo diálogo.
 * - Sin parámetros de entrada: el usuario es el de la sesión actual y la
 *   pantalla usa [AppContainer.authRepository] si no se inyecta un
 *   [ChangePasswordViewModel] (tests/preview).
 *
 * Valida en la app antes de llamar al API (mínimo 8 caracteres, confirmación
 * igual, nueva distinta a la actual) y muestra el error del API con
 * `networkMessage()`.
 */
@Composable
fun ChangePasswordScreen(
    onDone: () -> Unit,
    viewModel: ChangePasswordViewModel = viewModel { ChangePasswordViewModel(AppContainer.authRepository) },
) {
    val state by viewModel.uiState.collectAsState()
    ChangePasswordContent(
        state = state,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onSubmit = { viewModel.submit(onDone) },
    )
}

@Composable
private fun ChangePasswordContent(
    state: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
) {
    // Misma superficie que el login: color de hoja para que el contenido corto no deje hueco.
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ChangePasswordBrandHeader()
            ChangePasswordFormSheet(
                state = state,
                onCurrentPasswordChange = onCurrentPasswordChange,
                onNewPasswordChange = onNewPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onSubmit = onSubmit,
            )
        }
    }
}

@Composable
private fun ChangePasswordBrandHeader() {
    BrandGradientBackdrop(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                // 28dp extra: es lo que cubre la hoja del formulario al solaparse.
                .padding(top = 24.dp, bottom = 28.dp + 28.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLogoBadge(size = 56.dp, cornerRadius = 18.dp, innerPadding = 8.dp)
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        "Puerto Nuevo",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    OnBrandChip(
                        label = "CONTRASEÑA PROVISIONAL",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Text(
                "Cambia tu contraseña para continuar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp),
            )
            Text(
                "Es el primer acceso con la contraseña que te entregaron.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun ChangePasswordFormSheet(
    state: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoSheet(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
        ) {
            StatusChip(
                label = "ACCESO RESTRINGIDO",
                tone = BentoTone.Sun,
                leadingIcon = Icons.Filled.Lock,
            )

            Text(
                "Define tu nueva contraseña",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Usa al menos $MIN_NEW_PASSWORD_LENGTH caracteres y algo que solo tú conozcas. " +
                    "La contraseña provisional dejará de servir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )

            AppTextField(
                value = state.currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = { Text("Contraseña actual") },
                singleLine = true,
                visualTransformation = passwordTransformation(state.passwordVisible),
                trailingIcon = { PasswordVisibilityToggle(state.passwordVisible, onTogglePasswordVisibility) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                testTag = "changePassword.current",
            )

            Spacer(Modifier.height(16.dp))

            AppTextField(
                value = state.newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("Nueva contraseña") },
                singleLine = true,
                visualTransformation = passwordTransformation(state.passwordVisible),
                trailingIcon = { PasswordVisibilityToggle(state.passwordVisible, onTogglePasswordVisibility) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                testTag = "changePassword.new",
            )

            Spacer(Modifier.height(16.dp))

            AppTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirmar nueva contraseña") },
                singleLine = true,
                visualTransformation = passwordTransformation(state.passwordVisible),
                trailingIcon = { PasswordVisibilityToggle(state.passwordVisible, onTogglePasswordVisibility) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
                testTag = "changePassword.confirm",
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            PillButton(
                text = "Guardar contraseña",
                onClick = onSubmit,
                loading = state.loading,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Transformación de los tres campos de contraseña (un solo interruptor de "ver"). */
@Composable
private fun passwordTransformation(visible: Boolean): VisualTransformation =
    if (visible) VisualTransformation.None else PasswordVisualTransformation()

@Composable
private fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Ocultar contraseñas" else "Mostrar contraseñas",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
