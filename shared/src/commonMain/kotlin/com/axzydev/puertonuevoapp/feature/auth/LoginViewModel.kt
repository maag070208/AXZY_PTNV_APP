package com.axzydev.puertonuevoapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de login (jetbrains lifecycle KMP, Android+iOS). La pantalla solo
 * lee [uiState] y despacha eventos; la lógica y el ciclo de vida del login
 * viven aquí y en [AuthRepository].
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(serverUrl = authRepository.getServerUrl()))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value) }

    fun toggleServerField() = _uiState.update { it.copy(showServerField = !it.showServerField) }

    fun login() {
        val state = _uiState.value
        if (state.loading) return
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa usuario y contraseña") }
            return
        }
        authRepository.setServerUrl(state.serverUrl.trim())
        _uiState.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.login(state.username.trim(), state.password)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "No se pudo iniciar sesión",
                    )
                }
            } else {
                // El VM sobrevive al logout (scoped al Activity): hay que limpiar
                // el estado para que al volver al login no quede el spinner.
                _uiState.update { it.copy(loading = false, password = "") }
            }
            // En éxito el AuthState de la sesión pasa a LoggedIn y App.kt
            // cambia solo al shell: aquí no hay nada que navegar.
        }
    }
}