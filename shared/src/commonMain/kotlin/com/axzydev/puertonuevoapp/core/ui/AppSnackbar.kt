package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.axzydev.puertonuevoapp.core.theme.AppColors
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Controlador global de avisos (snackbars). Cualquier capa (UI, repos, ApiClient)
 * puede emitir un mensaje sin depender de un host particular; `AppSnackbarHost`
 * — montado una sola vez en el shell — lo muestra.
 *
 * Equivalente móvil del toaster/mensajes centrales de la web; evita que cada
 * pantalla reinvente su propio aviso de error.
 */
object AppSnackbar {
    data class Message(
        val text: String,
        val isError: Boolean,
    )

    private val _messages = MutableSharedFlow<Message>(extraBufferCapacity = 32)
    val messages = _messages.asSharedFlow()

    fun show(text: String, isError: Boolean = false) {
        _messages.tryEmit(Message(text, isError))
    }

    fun showError(text: String) = show(text, isError = true)
}

/**
 * Único `SnackbarHost` de la app: recolecta los mensajes de `AppSnackbar`
 * secuencialmente (encola) y colorea de rojo los de error. Montar en el
 * `Scaffold` raíz del shell (AppShell).
 */
@Composable
fun AppSnackbarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val isError = remember { mutableStateOf(false) }

    LaunchedEffect(hostState) {
        AppSnackbar.messages.collect { msg ->
            isError.value = msg.isError
            hostState.showSnackbar(
                message = msg.text,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
        }
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            androidx.compose.material3.Snackbar(
                snackbarData = data,
                containerColor = if (isError.value) AppColors.Danger else AppColors.Surface,
                contentColor = if (isError.value) Color.White else AppColors.TextPrimary,
            )
        },
    )
}