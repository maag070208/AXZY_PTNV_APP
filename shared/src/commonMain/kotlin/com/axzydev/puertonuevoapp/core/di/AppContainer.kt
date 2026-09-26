package com.axzydev.puertonuevoapp.core.di

import com.axzydev.puertonuevoapp.core.network.auth.AuthApi
import com.axzydev.puertonuevoapp.core.network.access.AccessApi
import com.axzydev.puertonuevoapp.core.network.access.AccessApiClient
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.departments.DepartmentsApi
import com.axzydev.puertonuevoapp.core.network.audit.AuditApi
import com.axzydev.puertonuevoapp.core.network.custodyletters.CustodyLettersApi
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardApi
import com.axzydev.puertonuevoapp.core.network.devicetypes.DeviceTypesApi
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.network.locations.LocationsApi
import com.axzydev.puertonuevoapp.core.network.notifications.NotificationsApi
import com.axzydev.puertonuevoapp.core.network.permissions.PermissionsApi
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import com.axzydev.puertonuevoapp.core.network.reports.ReportsApi
import com.axzydev.puertonuevoapp.core.network.materialoutputs.MaterialOutputsApi
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import com.axzydev.puertonuevoapp.core.location.LocationProvider
import com.axzydev.puertonuevoapp.core.location.platformLocationProvider
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
    val authApi: AuthApi by lazy { AuthApi(apiClient) }
    val devicesApi: DevicesApi by lazy { DevicesApi(apiClient) }
    val dashboardApi: DashboardApi by lazy { DashboardApi(apiClient) }
    val deviceTypesApi: DeviceTypesApi by lazy { DeviceTypesApi(apiClient) }
    val ticketsApi: TicketsApi by lazy { TicketsApi(apiClient) }
    val usersApi: UsersApi by lazy { UsersApi(apiClient) }
    val hrApi: HrApi by lazy { HrApi(apiClient) }
    val departmentsApi: DepartmentsApi by lazy { DepartmentsApi(apiClient) }
    val locationsApi: LocationsApi by lazy { LocationsApi(apiClient) }
    val inventoryApi: InventoryApi by lazy { InventoryApi(apiClient) }
    val materialOutputsApi: MaterialOutputsApi by lazy { MaterialOutputsApi(apiClient) }
    val custodyLettersApi: CustodyLettersApi by lazy { CustodyLettersApi(apiClient) }
    val reportsApi: ReportsApi by lazy { ReportsApi(apiClient) }
    val notificationsApi: NotificationsApi by lazy { NotificationsApi(apiClient) }
    val auditApi: AuditApi by lazy { AuditApi(apiClient) }
    val permissionsApi: PermissionsApi by lazy { PermissionsApi(apiClient) }
    val accessApi: AccessApi by lazy { AccessApiClient(apiClient) }
    val locationProvider: LocationProvider by lazy { platformLocationProvider() }
}