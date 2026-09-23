package com.axzydev.puertonuevoapp.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Acción situada en el TopAppBar del shell (a la derecha), registrada por la
 * pantalla actual. El TopAppBar vive en `AppShell` (fuera del nav host), así
 * que un composable hijo no puede inyectarle actions directamente: la pantalla
 * registra su acción aquí (p.ej. abrir el modal de filtros de una tabla) y el
 * shell la pinta.
 */
data class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

class TopBarActionsState {
    var action by mutableStateOf<TopBarAction?>(null)
        private set

    fun set(action: TopBarAction) {
        this.action = action
    }

    fun clear(action: TopBarAction) {
        if (this.action === action) this.action = null
    }
}

val LocalTopBarActions = staticCompositionLocalOf { TopBarActionsState() }

/**
 * Pide al shell que muestre la acción en la barra superior mientras la pantalla
 * está compuesta; al salir se restaura. El `onClick` siempre apunta al último
 * valor pasado (evita capturar closures viejos en recomposiciones).
 */
@Composable
fun RegisterTopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val state = LocalTopBarActions.current
    val currentOnClick by rememberUpdatedState(onClick)
    DisposableEffect(state, icon, contentDescription) {
        val action = TopBarAction(icon, contentDescription) { currentOnClick() }
        state.set(action)
        onDispose { state.clear(action) }
    }
}