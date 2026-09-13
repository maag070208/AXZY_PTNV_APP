package com.axzydev.puertonuevoapp.feature.auth

/** Estado inmutable de la pantalla de login. */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val serverUrl: String = "",
    val showServerField: Boolean = false,
    val loading: Boolean = false,
    val errorMessage: String? = null,
)