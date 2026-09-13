package com.axzydev.puertonuevoapp.core.network

import com.axzydev.puertonuevoapp.core.session.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP delgado sobre Ktor: agrega el token de sesión, resuelve la
 * URL base configurada por el usuario, y decodifica/lanza errores del
 * backend de forma consistente en todos los módulos.
 */
class ApiClient(
    private val tokenStore: TokenStore
) {
    /**
     * Handler global de sesión expirada — equivalente a los hooks de
     * `web/src/shared/api/session.ts` (`handleUnauthorized`). `AppContainer`
     * lo conecta a `AuthRepository.logout()`: cualquier respuesta 401 del
     * backend limpia la sesión y regresa al login automáticamente.
     */
    var onUnauthorized: () -> Unit = {}

    @PublishedApi
    internal val json: Json = Json {
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

    @PublishedApi
    internal val httpClient: HttpClient = HttpClient {
        expectSuccess = false

        install(Logging) {
            level = LogLevel.INFO
        }
    }

    fun currentBaseUrl(): String =
        tokenStore.getServerUrl()
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf { it.isNotBlank() }
            ?: ApiConfig.DEFAULT_BASE_URL

    @PublishedApi
    internal fun buildUrl(path: String): String =
        currentBaseUrl() + path

    @PublishedApi
    internal fun HttpRequestBuilder.authHeader() {
        tokenStore.getToken()?.let { token ->
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

    suspend inline fun <reified B, reified T> post(
        path: String,
        body: B
    ): T {
        val response = httpClient.post(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }

        return decode(response)
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

    // Para DELETEs que responden 204 sin cuerpo (ej. `/cartas/:id`): decode<Unit>
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
