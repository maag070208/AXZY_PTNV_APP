package com.axzydev.puertonuevoapp.core.network

object ApiConfig {
    /**
     * URL base por defecto del API mientras no se despliegue en un dominio.
     * Editable sin recompilar desde la pantalla de login ("Servidor").
     */
    const val DEFAULT_BASE_URL = "http://192.168.10.100:4001/api/v1"
}

class ApiException(val status: Int, message: String) : Exception(message)

/**
 * Convierte una excepción lanzada por la capa de red en un mensaje amigable:
 * los errores del backend (`ApiException`) ya traen su `message` en español;
 * cualquier otra cosa (sin conexión, timeouts, DNS…) se resume en un aviso claro
 * — evita leaks de mensajes internos de la librería (inglés/códigos) hacia la UI.
 */
fun networkMessage(e: Throwable): String = when (e) {
    is ApiException -> e.message ?: "Error ${e.status}"
    else -> "No se pudo conectar al servidor. Revisa tu conexión o la URL configurada."
}
