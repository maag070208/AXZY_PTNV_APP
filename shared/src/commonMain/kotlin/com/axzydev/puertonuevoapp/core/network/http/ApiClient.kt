package com.axzydev.puertonuevoapp.core.network.http

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** JSON del API (el mismo para la app y sus pruebas). */
internal val apiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
    // Los endpoints de update son parciales (zod `.optional()` en el backend,
    // sin `.nullable()` salvo un puñado de campos). Con encodeDefaults=true
    // pero explicitNulls por default, cualquier campo nulo del DTO (ej. los
    // que un formulario no tocó) se serializaba como "campo": null y el
    // backend lo rechazaba (400) por no aceptar null ahí. Al desactivar
    // explicitNulls, un campo nulo simplemente se omite — igual que no
    // haberlo tocado — que es la semántica correcta para un PUT parcial.
    explicitNulls = false
}

/**
 * Una parte binaria de un formulario `multipart/form-data`.
 *
 * El backend recibe los archivos con el nombre de campo `file` (multer
 * `upload.single("file")`) y los demás datos como campos de texto; por eso
 * [name] es el nombre del campo del formulario, no el nombre del archivo:
 *
 * ```kotlin
 * MultipartPart(name = "file", fileName = picked.name, mime = picked.mime, bytes = picked.bytes)
 * ```
 *
 * `PickedFile.toMultipartPart()` (en `core/platform/FilePicker.kt`) arma la
 * parte con los valores de un archivo elegido por el usuario.
 */
data class MultipartPart(
    /** Nombre del campo del formulario (siempre `file` para los endpoints del API). */
    val name: String,
    /** Nombre del archivo que ve el servidor (`Content-Disposition: filename`). */
    val fileName: String,
    /** MIME del archivo (`image/png`, `application/pdf`, `text/csv`, …). */
    val mime: String,
    /** Contenido binario completo del archivo. */
    val bytes: ByteArray,
)

/**
 * Cliente HTTP delgado sobre Ktor — capa de infraestructura, sin dependencias
 * de dominio/sesión (inversión de dependencias): recibe proveedores para el
 * token y la URL base; `AppContainer` los cablea desde el `TokenStore`.
 *
 * Agrega el bearer token, resuelve la base URL, y decodifica/lanza errores del
 * backend de forma consistente para todos los módulos de datos.
 */
class ApiClient(
    private val getToken: () -> String?,
    private val getServerUrl: () -> String?,
) {
    /**
     * Handler global de sesión expirada — equivalente a los hooks de
     * `web/src/shared/api/session.ts` (`handleUnauthorized`). `AppContainer`
     * lo conecta a `AuthRepository.logout()`: cualquier respuesta 401 del
     * backend limpia la sesión y regresa al login automáticamente.
     */
    var onUnauthorized: () -> Unit = {}

    @PublishedApi
    internal val json: Json = apiJson

    @PublishedApi
    internal val httpClient: HttpClient = HttpClient {
        expectSuccess = false

        // El API traduce sus mensajes (errores de validación/negocio) según
        // `Accept-Language` o, sin header, el idioma del sistema
        // (`sys_config.LANGUAGE`). La UI de la app está en español.
        defaultRequest {
            header(HttpHeaders.AcceptLanguage, "es")
        }

        install(Logging) {
            level = LogLevel.INFO
        }
    }

    fun currentBaseUrl(): String =
        getServerUrl()
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: ApiConfig.DEFAULT_BASE_URL

    @PublishedApi
    internal fun buildUrl(path: String): String =
        currentBaseUrl() + path

    @PublishedApi
    internal fun HttpRequestBuilder.authHeader() {
        getToken()?.let { token ->
            header(
                HttpHeaders.Authorization,
                "Bearer $token"
            )
        }
    }

    suspend inline fun <reified T> get(path: String): T {
        val response = httpClient.get(buildUrl(path)) {
            authHeader()
        }

        return decode(response)
    }

    /**
     * Descarga bytes crudos (imágenes/archivos). El endpoint de foto del
     * empleado (`/hr/:id/photo/raw`) vive bajo la misma base que el
     * resto del API; se resuelve con [buildUrl].
     */
    suspend fun getBytes(path: String): ByteArray {
        val response = httpClient.get(buildUrl(path)) {
            authHeader()
        }
        if (!response.status.isSuccess()) {
            if (response.status.value == 401) {
                onUnauthorized()
            }
            decode<ApiErrorBody>(response)
        }
        return response.readRawBytes()
    }

    /**
     * Descarga bytes con un POST que lleva los filtros en el cuerpo JSON: las
     * exportaciones CSV del API (`POST /access/report/export`,
     * `POST /time-clock/report/export`, `POST /schedules/overtime/export`).
     * `GET /reports/.csv` y las plantillas `.xlsx` usan [getBytes].
     */
    suspend inline fun <reified B> postBytes(path: String, body: B): ByteArray {
        val response = httpClient.post(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
        if (!response.status.isSuccess()) {
            decode<ApiErrorBody>(response)
        }
        return response.readRawBytes()
    }

    /**
     * POST con cuerpo JSON y respuesta JSON.
     *
     * [headers] agrega encabezados de la petición, pensados para cabeceras de
     * infraestructura como `Idempotency-Key` (el API deduplica altas,
     * movimientos, préstamos y devoluciones con esa clave). El `Authorization`
     * y el `Content-Type: application/json` los pone el cliente; no se pasan
     * aquí.
     */
    suspend inline fun <reified B, reified T> post(
        path: String,
        body: B,
        headers: Map<String, String> = emptyMap(),
    ): T {
        val response = httpClient.post(buildUrl(path)) {
            authHeader()
            headers.forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }

        return decode(response)
    }

    /**
     * Sube un formulario `multipart/form-data` y decodifica la respuesta JSON.
     *
     * [parts] son los archivos (normalmente uno con `name = "file"`, que es el
     * campo que lee multer en el API) y [fields] los campos de texto del mismo
     * formulario (p. ej. `kind` de los adjuntos de ticket, o las opciones de
     * una carga masiva). [headers] permite cabeceras como `Idempotency-Key`.
     *
     * El `Content-Type` con el `boundary` lo genera Ktor a partir del cuerpo;
     * no se pasa en [headers].
     */
    suspend inline fun <reified T> postMultipart(
        path: String,
        parts: List<MultipartPart>,
        fields: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): T {
        val response = multipartRequest(
            path = path,
            parts = parts,
            fields = fields,
            headers = headers,
        )

        return decode(response)
    }

    /**
     * Variante de [postMultipart] para endpoints que responden 204 (o cuyo
     * cuerpo de respuesta la UI relee del recurso padre): solo se decodifica
     * el error si la llamada no fue exitosa.
     */
    suspend fun postMultipartNoContent(
        path: String,
        parts: List<MultipartPart>,
        fields: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ) {
        val response = multipartRequest(
            path = path,
            parts = parts,
            fields = fields,
            headers = headers,
        )
        if (!response.status.isSuccess()) {
            decode<ApiErrorBody>(response)
        }
    }

    @PublishedApi
    internal suspend fun multipartRequest(
        path: String,
        parts: List<MultipartPart>,
        fields: Map<String, String>,
        headers: Map<String, String>,
    ): HttpResponse =
        httpClient.post(buildUrl(path)) {
            authHeader()
            headers.forEach { (name, value) -> header(name, value) }
            setBody(multipartBody(parts, fields))
        }

    // Para POSTs con cuerpo cuyo body de respuesta no interesa (ej. comentarios/
    // asignaciones de tickets, donde la UI refresca releyendo el recurso padre):
    // mismo motivo que putNoContent, decode<Unit> fallaría si el body no es "".
    suspend inline fun <reified B> postNoContent(path: String, body: B) {
        val response = httpClient.post(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
        if (!response.status.isSuccess()) {
            decode<ApiErrorBody>(response)
        }
    }

    suspend inline fun <reified T> postNoBody(path: String): T {
        val response = httpClient.post(buildUrl(path)) {
            authHeader()
        }

        return decode(response)
    }

    // Para POSTs sin cuerpo de petición NI de respuesta (204), como
    // /notifications/:id/read: mismo motivo que deleteNoContent, decode<Unit>
    // fallaría al intentar parsear un body vacío como JSON.
    suspend fun postNoContent(path: String) {
        val response = httpClient.post(buildUrl(path)) {
            authHeader()
        }
        if (!response.status.isSuccess()) {
            decode<ApiErrorBody>(response)
        }
    }

    suspend inline fun <reified B, reified T> put(
        path: String,
        body: B
    ): T {
        val response = httpClient.put(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }

        return decode(response)
    }

    suspend inline fun <reified B, reified T> patch(
        path: String,
        body: B
    ): T {
        val response = httpClient.patch(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }

        return decode(response)
    }

    suspend inline fun <reified B> putNoContent(path: String, body: B) {
        val response = httpClient.put(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
        if (!response.status.isSuccess()) {
            decode<ApiErrorBody>(response)
        }
    }

    suspend inline fun <reified T> delete(path: String): T {
        val response = httpClient.delete(buildUrl(path)) {
            authHeader()
        }

        return decode(response)
    }

    // Para DELETEs que responden 204 sin cuerpo (ej. `/notifications/:id`): decode<Unit>
    // fallaría al intentar parsear un body vacío como JSON. Mismo patrón que
    // putNoContent: solo se decodifica el error si la llamada no fue exitosa.
    suspend fun deleteNoContent(path: String) {
        val response = httpClient.delete(buildUrl(path)) {
            authHeader()
        }
        if (!response.status.isSuccess()) {
            decode<ApiErrorBody>(response)
        }
    }

    /**
     * Cuerpo `multipart/form-data`: [fields] son los campos de texto y
     * [parts] los archivos. Cada archivo lleva su MIME y su nombre de archivo;
     * Ktor les antepone `name=<campo>` en la misma cabecera
     * `Content-Disposition` y calcula el `Content-Length` de la petición.
     */
    @PublishedApi
    internal fun multipartBody(
        parts: List<MultipartPart>,
        fields: Map<String, String>,
    ): MultiPartFormDataContent =
        MultiPartFormDataContent(
            formData {
                fields.forEach { (name, value) -> append(name, value) }
                parts.forEach { part ->
                    append(
                        part.name,
                        part.bytes,
                        Headers.build {
                            append(
                                HttpHeaders.ContentType,
                                part.mime.ifBlank { "application/octet-stream" },
                            )
                            // Ktor ya puso `form-data; name=<campo>`; aquí solo
                            // va el nombre del archivo (se concatenan en una
                            // sola cabecera Content-Disposition).
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=\"${fileNameHeader(part.fileName)}\"",
                            )
                        },
                    )
                }
            },
        )

    @PublishedApi
    internal suspend inline fun <reified T> decode(
        response: HttpResponse
    ): T {
        val text = response.bodyAsText()

        if (!response.status.isSuccess()) {
            if (response.status.value == 401) {
                onUnauthorized()
            }
            val message = runCatching {
                json
                    .decodeFromString(
                        ApiErrorBody.serializer(),
                        text
                    )
                    .message
            }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: text.ifBlank {
                    "Error ${response.status.value}"
                }

            throw ApiException(
                response.status.value,
                message
            )
        }

        return json.decodeFromString(text)
    }
}
/**
 * Nombre de archivo seguro para la cabecera `Content-Disposition`: sin rutas,
 * sin CR/LF (que cortarían el encabezado) y sin comillas dobles (que lo
 * romperían). Si queda vacío se usa `archivo`.
 */
private fun fileNameHeader(fileName: String): String =
    fileName
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace("\"", "'")
        .replace("\r", "")
        .replace("\n", "")
        .trim()
        .ifBlank { "archivo" }
