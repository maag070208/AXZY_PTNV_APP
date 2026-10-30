package com.axzydev.puertonuevoapp.feature.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axzydev.puertonuevoapp.core.network.http.networkMessage
import com.axzydev.puertonuevoapp.core.network.users.UsersApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Historial de una cuenta (`GET /users/:id` + `GET /users/:id/history`): la
 * ficha de la persona y sus eventos reales (inventario, tickets y altas/bajas),
 * con filtro por familia de evento.
 */
class UserHistoryViewModel(
    private val userId: String,
    private val usersApi: UsersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserHistoryUiState())
    val uiState: StateFlow<UserHistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Relee la ficha y el historial en paralelo (pantalla y botón de reintento). */
    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                coroutineScope {
                    val user = async { usersApi.get(userId) }
                    val history = async { usersApi.history(userId) }
                    user.await() to history.await()
                }
            }
            result.fold(
                onSuccess = { (user, entries) ->
                    _uiState.update {
                        it.copy(loading = false, error = null, user = user, entries = entries)
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(loading = false, error = networkMessage(error)) }
                },
            )
        }
    }

    /** Píldora de familia del feed (no vuelve a pedir datos: filtra en memoria). */
    fun onFamilyChange(family: HistoryFamily) = _uiState.update { it.copy(family = family) }
}
