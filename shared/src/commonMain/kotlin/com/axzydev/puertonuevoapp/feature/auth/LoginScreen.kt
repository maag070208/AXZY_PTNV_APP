package com.axzydev.puertonuevoapp.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel { LoginViewModel(AppContainer.authRepository) }) {
    val state by viewModel.uiState.collectAsState()
    LoginContent(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onServerUrlChange = viewModel::onServerUrlChange,
        onToggleServer = viewModel::toggleServerField,
        onLogin = viewModel::login,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onServerUrlChange: (String) -> Unit,
    onToggleServer: () -> Unit,
    onLogin: () -> Unit,
) {
    // Superficie de fondo = color de la hoja: si el contenido es corto no queda hueco.
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        val scrollState = rememberScrollState()
        // Con el teclado abierto el formulario se desplaza hasta abajo: así el campo
        // enfocado y el botón «Entrar» quedan por encima del teclado, nunca cortados.
        val density = LocalDensity.current
        val imeBottom = WindowInsets.ime.getBottom(density)
        LaunchedEffect(imeBottom) {
            if (imeBottom > 0) scrollState.animateScrollTo(scrollState.maxValue)
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            LoginBrandHeader()
            LoginFormSheet(
                state = state,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onServerUrlChange = onServerUrlChange,
                onToggleServer = onToggleServer,
                onLogin = onLogin,
            )
        }
    }
}

/** Característica destacada del producto (chip translúcido del encabezado). */
private data class BrandFeature(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val brandFeatures = listOf(
    BrandFeature("Inventario y responsivas", Icons.Filled.Inventory2),
    BrandFeature("Tickets y tareas", Icons.Filled.ConfirmationNumber),
    BrandFeature("Asistencia y RH", Icons.Filled.Groups),
    BrandFeature("Notificaciones", Icons.Filled.Notifications),
)

@Composable
private fun LoginBrandHeader() {
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
                BrandLogoBadge(size = 64.dp, cornerRadius = 20.dp, innerPadding = 9.dp, floating = true)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        "Puerto Nuevo",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    OnBrandChip(
                        label = "HOTEL & VILLAS · OPERACIÓN",
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Text(
                "Toda la operación del hotel, en un solo lugar.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
            )

            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                brandFeatures.forEach { OnBrandChip(label = it.label, icon = it.icon) }
            }
        }
    }
}

@Composable
private fun LoginFormSheet(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onServerUrlChange: (String) -> Unit,
    onToggleServer: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Cada campo se trae a la vista al enfocarse (scroll del padre + teclado).
    val usernameIntoView = remember { BringIntoViewRequester() }
    val passwordIntoView = remember { BringIntoViewRequester() }
    val serverIntoView = remember { BringIntoViewRequester() }

    BentoSheet(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
        ) {
            StatusChip(label = "ACCESO SEGURO", tone = BentoTone.Sea, leadingIcon = Icons.Filled.Lock)

            Text(
                "Bienvenido de nuevo",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Inicia sesión para continuar con la operación del hotel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            AppTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = { Text("Usuario") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                testTag = "login.username",
                bringIntoView = usernameIntoView,
            )

            Spacer(Modifier.height(16.dp))

            AppTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Contraseña") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (state.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onLogin() }),
                modifier = Modifier.fillMaxWidth(),
                testTag = "login.password",
                bringIntoView = passwordIntoView,
            )

            AnimatedVisibility(visible = state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            PillButton(
                text = "Entrar",
                onClick = onLogin,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(
                onClick = onToggleServer,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            ) {
                Text(
                    "Configuración del servidor",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Icon(
                    Icons.Filled.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp).size(16.dp),
                )
            }

            AnimatedVisibility(visible = state.showServerField) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    AppTextField(
                        value = state.serverUrl,
                        onValueChange = onServerUrlChange,
                        label = { Text("URL del API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "login.serverUrl",
                        bringIntoView = serverIntoView,
                    )
                    Text(
                        "Ej. http://192.168.1.50:4001/api/v1 (IP local de la Mac). " +
                            "Con el teléfono conectado por USB funciona http://localhost:4001/api/v1 " +
                            "usando adb reverse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                    )
                }
            }

            Text(
                "Puerto Nuevo · Cartas Responsivas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
    }
}
