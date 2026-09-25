package com.axzydev.puertonuevoapp.feature.credential

/** Credencial digital del usuario: el QR sale de la sesión y los datos, de `/auth/me`. */
data class MyCredentialUiState(
    val qrData: String,
    val name: String,
    val role: String,
    val loading: Boolean = true,
    val numeroEmpleado: String? = null,
    val puesto: String? = null,
    val department: String? = null,
    val fotoUrl: String? = null,
    /** Si `/auth/me` falla, el QR sigue sirviendo (solo lleva el id). */
    val error: String? = null,
)
