package com.axzydev.puertonuevoapp.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.axzydev.puertonuevoapp.core.di.AppContainer
import com.axzydev.puertonuevoapp.core.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val authRepository = AppContainer.authRepository
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(authRepository.getServerUrl()) }
    var showServerField by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun doLogin() {
        if (username.isBlank() || password.isBlank() || loading) return
        errorMessage = null
        loading = true
        authRepository.setServerUrl(serverUrl)
        scope.launch {
            val result = authRepository.login(username.trim(), password)
            loading = false
            result.onFailure { e ->
                errorMessage = e.message ?: "No se pudo iniciar sesión"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(AppColors.EmeraldContainer, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Business,
                    contentDescription = null,
                    tint = AppColors.EmeraldOnContainer,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Puerto Nuevo",
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.TextPrimary,
            )
            Text(
                "Hotel y Villas — Seguimiento operativo",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.EmeraldPrimary),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.EmeraldPrimary),
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = AppColors.Danger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { doLogin() },
                enabled = !loading,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.EmeraldPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppColors.Surface, strokeWidth = 2.dp)
                } else {
                    Text("Entrar", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(20.dp))

            TextButton(onClick = { showServerField = !showServerField }) {
                Icon(Icons.Filled.Dns, contentDescription = null, tint = AppColors.TextFaint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.height(0.dp))
                Text(
                    "  Configuración del servidor",
                    color = AppColors.TextFaint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            AnimatedVisibility(visible = showServerField) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("URL del API") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Ej. http://192.168.1.50:4001/api/v1 (IP local de la Mac)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextFaint,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
