package com.axzydev.puertonuevoapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.notifications.NotificationsApi
import com.axzydev.puertonuevoapp.core.network.inventory.InventoryApi
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import com.axzydev.puertonuevoapp.core.session.AuthState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del Inicio (clon de la web). Pide en paralelo solo los bloques que los permisos de
 * la sesión permiten (el resumen requiere `dashboard.view`; cada widget, su permiso;
 * "mi equipo" solo la sesión). Un bloque que falla no tumba a los demás.
 */
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val inventoryApi: InventoryApi,
    private val dashboardApi: DashboardApi,
    private val notificationsApi: NotificationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val user = (authRepository.state.value as? AuthState.LoggedIn)?.user
        _uiState.update { it.copy(loading = true, errors = emptyMap()) }
        viewModelScope.launch {
            coroutineScope {
                buildList {
                    // Conteo de no leídas para la campana; si falla, simplemente no hay insignia.
                    add(async {
                        runCatching { notificationsApi.unreadCount() }
                            .onSuccess { n -> _uiState.update { it.copy(unreadNotifications = n) } }
                    })
                    if (user?.canViewDashboard == true) {
                        add(async { fetch(HomeSection.Summary, { dashboardApi.summary() }) { s, v -> s.copy(summary = v) } })
                        // Categorías: opcional, igual que la web (sin permiso de inventario se omite).
                        add(async {
                            runCatching { inventoryApi.deviceCategories() }
                                .onSuccess { v -> _uiState.update { it.copy(categories = v) } }
                        })
                    }
                    // Sin `dashboard.view` la pestaña Operación cae a los widgets de tickets y tareas.
                    if (user?.canViewTickets == true && !user.canViewDashboard) {
                        add(async { fetch(HomeSection.Tickets, { dashboardApi.tickets() }) { s, v -> s.copy(tickets = v) } })
                    }
                    if (user?.canViewTasks == true && !user.canViewDashboard) {
                        add(async { fetch(HomeSection.Tasks, { dashboardApi.tasks() }) { s, v -> s.copy(tasks = v) } })
                    }
                    // "Mi espacio" solo requiere sesión.
                    add(async { fetch(HomeSection.Equipment, { dashboardApi.myEquipment() }) { s, v -> s.copy(equipment = v) } })
                    // Personal: con `hr.records` la web arma el mosaico completo (asistencia y horas extra incluidas).
                    if (user?.canViewAttendance == true) {
                        add(async { fetch(HomeSection.Attendance, { dashboardApi.attendanceToday() }) { s, v -> s.copy(attendance = v) } })
                    }
                    if (user?.canViewOvertime == true) {
                        add(async { fetch(HomeSection.Overtime, { dashboardApi.overtimeWeek() }) { s, v -> s.copy(overtime = v) } })
                    }
                    if (user?.canViewHR == true) {
                        add(async { fetch(HomeSection.HrRecords, { dashboardApi.hrRecords() }) { s, v -> s.copy(hrRecords = v) } })
                    }
                    if (user?.canLinkTimeClock == true) {
                        add(async { fetch(HomeSection.SetupGaps, { dashboardApi.setupGaps() }) { s, v -> s.copy(setupGaps = v) } })
                    }
                    if (user?.canScanCredential == true) {
                        add(async { fetch(HomeSection.AccessToday, { dashboardApi.accessToday() }) { s, v -> s.copy(accessToday = v) } })
                    }
                    if (user?.canConfigureSystem == true) {
                        add(async { fetch(HomeSection.SystemHealth, { dashboardApi.systemHealth() }) { s, v -> s.copy(systemHealth = v) } })
                    }
                }.awaitAll()
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    private suspend fun <T> fetch(
        section: HomeSection,
        call: suspend () -> T,
        set: (HomeUiState, T) -> HomeUiState,
    ) {
        runCatching { call() }
            .onSuccess { value -> _uiState.update { set(it, value) } }
            .onFailure { e -> _uiState.update { it.copy(errors = it.errors + (section to networkMessage(e))) } }
    }
}
