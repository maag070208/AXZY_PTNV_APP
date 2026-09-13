package com.axzydev.puertonuevoapp.core.di

import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import com.axzydev.puertonuevoapp.core.session.TokenStore

/**
 * Composition root minimalista (sin librería de inyección de dependencias
 * para no depender de más artefactos de red que Gradle no puede resolver
 * desde este entorno). Todo son singletons perezosos de proceso.
 */
object AppContainer {
    private val unauthorizedActions = mutableListOf<() -> Unit>()

    val tokenStore: TokenStore by lazy { TokenStore() }
    val apiClient: ApiClient by lazy {
        ApiClient(
            getToken = { tokenStore.getToken() },
            getServerUrl = { tokenStore.getServerUrl() },
        ).also {
            it.onUnauthorized = { unauthorizedActions.forEach { action -> action() } }
        }
    }
    val authRepository: AuthRepository by lazy {
        AuthRepository(tokenStore, apiClient).also {
            unauthorizedActions += { it.logout() }
        }
    }
    val devicesApi: DevicesApi by lazy { DevicesApi(apiClient) }
}