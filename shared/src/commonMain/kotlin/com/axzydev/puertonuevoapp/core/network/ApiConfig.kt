package com.axzydev.puertonuevoapp.core.network

object ApiConfig {
    /**
     * URL base por defecto del API mientras no se despliegue en un dominio.
     * - Emulador Android: 10.0.2.2 apunta al localhost de la Mac.
     * - Celular físico en la misma red Wi-Fi: cambia esto por la IP LAN de
     *   la Mac (ej. "http://192.168.1.50:4001/api/v1"). Editable sin
     *   recompilar desde la pantalla de login ("Servidor").
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:4001/api/v1"
}

class ApiException(val status: Int, message: String) : Exception(message)
