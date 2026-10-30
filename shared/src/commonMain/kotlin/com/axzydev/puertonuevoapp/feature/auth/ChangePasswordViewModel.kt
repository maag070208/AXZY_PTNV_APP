package com.axzydev.puertonuevoapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mínimo de la contraseña nueva que exige la app (el API acepta desde 6). */
internal const val MIN_NEW_PASSWORD_LENGTH = 8

/** Estado inmutable de la pantalla de cambio de contraseña. */
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** El botón solo se habilita con los tres campos capturados. */
    val canSubmit: Boolean
        get() = currentPassword.isNotBlank() &&
            newPassword.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            !loading
}

/**
 * ViewModel de la pantalla de contraseña provisional (`mustChangePassword`).
 *
 * Valida en la app (mínimo 8 caracteres, confirmación igual, nueva distinta a
 * la actual) y delega en [AuthRepository.changePassword]; en éxito el
 * repositorio apaga `SessionUser.mustChangePassword` y el shell aparece solo,
 * pero de todos modos se invoca `onSuccess` para que la navegación del host
 * pueda reaccionar.
 */
class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(value: String) =
        _uiState.update { it.copy(currentPassword = value, errorMessage = null) }

    fun onNewPasswordChange(value: String) =
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    /**
     * Valida y envía el cambio. En éxito limpia los campos y llama a
     * [onSuccess] (la pantalla lo conecta con su `onDone`).
     */
    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.loading) return

        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.changePassword(state.currentPassword, state.newPassword)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                    )
                }
                onSuccess()
            } else {
                val error = result.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = error?.let(::networkMessage) ?: "No se pudo cambiar la contraseña.",
                    )
                }
            }
        }
    }

    private fun validate(state: ChangePasswordUiState): String? = when {
        state.currentPassword.isBlank() -> "Ingresa tu contraseña actual."
        state.newPassword.length < MIN_NEW_PASSWORD_LENGTH ->
            "La nueva contraseña debe tener al menos $MIN_NEW_PASSWORD_LENGTH caracteres."
        state.newPassword != state.confirmPassword -> "La nueva contraseña y la confirmación no coinciden."
        state.newPassword == state.currentPassword -> "La nueva contraseña debe ser distinta a la actual."
        else -> null
    }
}
