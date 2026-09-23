package com.axzydev.puertonuevoapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.dashboard.DashboardApi
import com.axzydev.puertonuevoapp.core.network.devices.DevicesApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.session.AuthRepository
import com.axzydev.puertonuevoapp.core.session.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del home. Para ADMIN/GERENTE carga el panel administrativo
 * (`/dashboard/summary`); para el resto, el resumen de inventario.
 */
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val devicesApi: DevicesApi,
    private val dashboardApi: DashboardApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val userName: String
        get() = (authRepository.state.value as? AuthState.LoggedIn)?.user?.name ?: ""

    val canSeeDashboard: Boolean
        get() = (authRepository.state.value as? AuthState.LoggedIn)?.user?.canSeeDashboard == true

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (canSeeDashboard) {
                val result = runCatching { dashboardApi.summary() }
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.let(::networkMessage),
                        dashboard = result.getOrNull(),
                    )
                }
            } else {
                val result = runCatching { devicesApi.summary() }
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.let(::networkMessage),
                        summary = result.getOrNull(),
                    )
                }
            }
        }
    }
}
