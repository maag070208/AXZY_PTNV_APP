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
class ApiClient(private val tokenStore: TokenStore) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val httpClient: HttpClient = HttpClient {
        expectSuccess = false
        install(Logging) { level = LogLevel.INFO }
    }

    fun currentBaseUrl(): String =
        tokenStore.getServerUrl()?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
            ?: ApiConfig.DEFAULT_BASE_URL

    private fun buildUrl(path: String): String = currentBaseUrl() + path

    private fun HttpRequestBuilder.authHeader() {
        tokenStore.getToken()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    suspend inline fun <reified T> get(path: String): T {
        val response = httpClient.get(buildUrl(path)) {
            authHeader()
        }
        return decode(response)
    }

    suspend inline fun <reified B, reified T> post(path: String, body: B): T {
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

    suspend inline fun <reified B, reified T> put(path: String, body: B): T {
        val response = httpClient.put(buildUrl(path)) {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        }
        return decode(response)
    }

    suspend inline fun <reified T> delete(path: String): T {
        val response = httpClient.delete(buildUrl(path)) {
            authHeader()
        }
        return decode(response)
    }

    suspend inline fun <reified T> decode(response: HttpResponse): T {
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val message = runCatching {
                json.decodeFromString(ApiErrorBody.serializer(), text).message
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: text.ifBlank { "Error ${response.status.value}" }
            throw ApiException(response.status.value, message)
        }
        return json.decodeFromString(text)
    }
}
