package com.axzydev.puertonuevoapp.feature.access

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Versión del esquema de credencial que el backend acepta hoy (`v:2`). */
const val CREDENTIAL_VERSION = 2

/**
 * Contenido del QR de credencial `v:2`: JSON compacto UTF-8 con `v` (versión)
 * e `id` (User.id); `no`/`name`/`pos`/`dept` son opcionales. Espejo del
 * contrato `web/src/widgets/credencial-empleado/model/buildQrPayload.ts` y del
 * parser del backend (`api/.../access.service.ts`).
 */
data class CredentialPayload(
    val version: Int,
    val employeeId: String,
    val numeroEmpleado: String? = null,
    val name: String? = null,
    val puesto: String? = null,
    val department: String? = null,
)

sealed interface QrParseResult {
    data class Valid(val payload: CredentialPayload) : QrParseResult
    data class Invalid(val reason: String) : QrParseResult
}

private val qrJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Parsea el payload crudo del QR. Reproduce la validación del servidor:
 * JSON objeto, `v` numérico igual a [CREDENTIAL_VERSION], `id` no vacío.
 */
fun parseCredentialPayload(raw: String): QrParseResult {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return QrParseResult.Invalid("El código QR está vacío")

    val element = runCatching { qrJson.parseToJsonElement(trimmed) }.getOrNull()
        ?: return QrParseResult.Invalid("El código QR no es un JSON válido")
    val obj = element as? JsonObject
        ?: return QrParseResult.Invalid("El código QR no tiene el formato esperado")

    val version = readVersion(obj["v"])
        ?: return QrParseResult.Invalid("El código QR no incluye la versión del esquema")
    if (version != CREDENTIAL_VERSION) {
        return QrParseResult.Invalid("Versión de credencial no soportada (v:$version)")
    }

    val employeeId = readString(obj["id"])
        ?: return QrParseResult.Invalid("El código QR no incluye el identificador del empleado")

    return QrParseResult.Valid(
        CredentialPayload(
            version = version,
            employeeId = employeeId,
            numeroEmpleado = readString(obj["no"]),
            name = readString(obj["name"]),
            puesto = readString(obj["pos"]),
            department = readString(obj["dept"]),
        )
    )
}

private fun readVersion(element: JsonElement?): Int? {
    val value = (element as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: return null
    if (!value.isFinite() || value != value.toInt().toDouble()) return null
    return value.toInt()
}

private fun readString(element: JsonElement?): String? =
    (element as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
