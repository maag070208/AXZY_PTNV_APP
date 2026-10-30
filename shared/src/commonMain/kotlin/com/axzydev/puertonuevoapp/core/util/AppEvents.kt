package com.axzydev.puertonuevoapp.core.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bus mínimo de **eventos locales en memoria** para avisar a otras pantallas que
 * algo cambió (p. ej. una tarea creada desde el panel de Langostín).
 *
 * Reglas del patrón:
 * - **La API sigue siendo la fuente de verdad.** Esto no guarda datos: solo una
 *   marca ("versión") que se incrementa para que quien escuche vuelva a pedirle
 *   la verdad al servidor.
 * - **Sin dependencias nuevas**: solo `kotlinx.coroutines.flow`.
 * - **Sin fugas**: nada se acumula por pantalla; el registro por clave se crea
 *   cuando alguien empieza a observar esa clave y vive lo que el proceso.
 * - Se emite desde la UI (o desde una corrutina de `viewModelScope`, que corre en
 *   el hilo principal), que es donde ocurren las acciones que avisan.
 *
 * Uso típico en una pantalla:
 * ```kotlin
 * val version by AppEvents.version("tickets:changed").collectAsState()
 * LaunchedEffect(version) {
 *     if (version != null) viewModel.load()
 * }
 * ```
 * Y donde ocurre el cambio: `AppEvents.emit("tickets:changed")`.
 */
object AppEvents {
    /** Contador por clave. Empieza vacío: ninguna clave tiene versión todavía. */
    private val flows = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Flujo por clave ya creado (el mismo objeto para todos los observadores). */
    private val versions = mutableMapOf<String, MutableStateFlow<Long?>>()

    /**
     * Versión observable de [key]: `null` hasta que alguien emita un evento con
     * esa clave (así una pantalla no recarga en su primera composición) y luego
     * un entero creciente por cada evento.
     */
    fun version(key: String): StateFlow<Long?> = versions.getOrPut(key) {
        MutableStateFlow(flows.value[key])
    }.asStateFlow()

    /** Avísale a quien escuche [key] que algo cambió (suma 1 a su versión). */
    fun emit(key: String) {
        val current = flows.value
        val next = (current[key] ?: 0L) + 1L
        flows.value = current + (key to next)
        // Si nadie observa la clave todavía, `version(key)` arrancará con el valor
        // ya incrementado porque lo lee de `flows`.
        versions[key]?.value = next
    }

    /** Última versión de [key] (`0` si nunca hubo un evento con ella). */
    fun currentVersion(key: String): Long = flows.value[key] ?: 0L
}
