package com.axzydev.puertonuevoapp.core.nav

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Navegación mínima sin librería externa: una pila observable por Compose.
 * Los tabs del bottom nav "resetean" la pila a su raíz (no se acumula
 * historial cruzado entre tabs); dentro de un tab, push/pop navegan a detalle.
 */
class Navigator(start: Screen) {
    val backStack: SnapshotStateList<Screen> = mutableStateListOf(start)

    val current: Screen get() = backStack.last()

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** Usado por el bottom nav: limpia el historial del tab actual y entra a [screen]. */
    fun switchTab(screen: Screen) {
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        backStack[0] = screen
    }
}

val LocalNavigator = compositionLocalOf<Navigator> {
    error("Navigator no provisto — envuelve la app con CompositionLocalProvider(LocalNavigator provides ...)")
}
