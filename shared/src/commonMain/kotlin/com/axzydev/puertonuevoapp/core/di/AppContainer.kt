package com.axzydev.puertonuevoapp.core.di

import com.axzydev.puertonuevoapp.core.network.ApiClient
import com.axzydev.puertonuevoapp.core.network.DevicesApi
import com.axzydev.puertonuevoapp.core.network.TicketsApi
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import com.axzydev.puertonuevoapp.core.session.TokenStore

/**
 * Composition root minimalista (sin librería de inyección de dependencias
 * para no depender de más artefactos de red que Gradle no puede resolver
 * desde este entorno). Todo son singletons perezosos de proceso.
 */
object AppContainer {
    val tokenStore: TokenStore by lazy { TokenStore() }
    val apiClient: ApiClient by lazy { ApiClient(tokenStore) }
    val authRepository: AuthRepository by lazy { AuthRepository(tokenStore, apiClient) }
    val ticketsApi: TicketsApi by lazy { TicketsApi(apiClient) }
    val devicesApi: DevicesApi by lazy { DevicesApi(apiClient) }
}
