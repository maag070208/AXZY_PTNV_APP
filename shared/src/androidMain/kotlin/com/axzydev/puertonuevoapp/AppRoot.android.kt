package com.axzydev.puertonuevoapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Raíz de la app en Android. Publica las etiquetas de prueba (`testTag`) como
 * `resource-id` de accesibilidad para que las pruebas E2E de Maestro puedan
 * seleccionar campos sin coordenadas (solo lectura para el árbol de accesibilidad).
 */
@Composable
fun AppRoot() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        App()
    }
}
