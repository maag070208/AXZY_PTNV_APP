package com.axzydev.puertonuevoapp.feature.credential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.auth.AuthApi
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.session.SessionUser
import com.axzydev.puertonuevoapp.feature.access.buildCredentialQr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** El QR está listo al instante (id de la sesión); foto, número y puesto llegan después. */
class MyCredentialViewModel(
    user: SessionUser,
    private val authApi: AuthApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MyCredentialUiState(qrData = buildCredentialQr(user.id), name = user.name, role = user.role),
    )
    val uiState: StateFlow<MyCredentialUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { authApi.me() }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { me ->
                        state.copy(
                            loading = false,
                            name = me.name,
                            numeroEmpleado = me.numeroEmpleado,
                            puesto = me.puesto,
                            department = me.department?.name,
                            fotoUrl = me.fotoUrl,
                        )
                    },
                    onFailure = { state.copy(loading = false, error = networkMessage(it)) },
                )
            }
        }
    }
}
