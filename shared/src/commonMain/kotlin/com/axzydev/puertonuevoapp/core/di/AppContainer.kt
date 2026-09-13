package com.axzydev.puertonuevoapp.core.di

import com.axzydev.puertonuevoapp.core.network.ApiClient
import com.axzydev.puertonuevoapp.core.network.AuditApi
import com.axzydev.puertonuevoapp.core.network.CartasApi
import com.axzydev.puertonuevoapp.core.network.DevicesApi
import com.axzydev.puertonuevoapp.core.network.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.InventoryApi
import com.axzydev.puertonuevoapp.core.network.LocationsApi
import com.axzydev.puertonuevoapp.core.network.NotificationsApi
import com.axzydev.puertonuevoapp.core.network.ReportsApi
import com.axzydev.puertonuevoapp.core.network.SalidasApi
import com.axzydev.puertonuevoapp.core.network.TicketsApi
import com.axzydev.puertonuevoapp.core.network.UsersApi
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
        ApiClient(tokenStore).also {
            it.onUnauthorized = { unauthorizedActions.forEach { action -> action() } }
        }
    }
    val authRepository: AuthRepository by lazy {
        AuthRepository(tokenStore, apiClient).also {
            unauthorizedActions += { it.logout() }
        }
    }
    val ticketsApi: TicketsApi by lazy { TicketsApi(apiClient) }
    val devicesApi: DevicesApi by lazy { DevicesApi(apiClient) }
    val deviceTypesApi: DeviceTypesApi by lazy { DeviceTypesApi(apiClient) }
    val usersApi: UsersApi by lazy { UsersApi(apiClient) }
    val departmentsApi: DepartmentsApi by lazy { DepartmentsApi(apiClient) }
    val locationsApi: LocationsApi by lazy { LocationsApi(apiClient) }
    val inventoryApi: InventoryApi by lazy { InventoryApi(apiClient) }
    val salidasApi: SalidasApi by lazy { SalidasApi(apiClient) }
    val cartasApi: CartasApi by lazy { CartasApi(apiClient) }
    val notificationsApi: NotificationsApi by lazy { NotificationsApi(apiClient) }
    val auditApi: AuditApi by lazy { AuditApi(apiClient) }
    val reportsApi: ReportsApi by lazy { ReportsApi(apiClient) }
}
