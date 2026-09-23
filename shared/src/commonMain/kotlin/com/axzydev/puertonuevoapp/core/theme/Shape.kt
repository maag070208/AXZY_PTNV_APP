package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Radios estándar de la app. Un solo punto de verdad para las esquinas de
 * tarjetas, filas de menú/lista e íconos contenedores. Al cambiar aquí, toda
 * la app queda consistente.
 */
object AppShape {
    /** Tarjetas y tiles (accesos rápidos, stat cards, surface cards). */
    val card = RoundedCornerShape(16.dp)

    /** Filas pulsables (drawer, listas, menús). */
    val row = RoundedCornerShape(12.dp)

    /** Contenedor de íconos dentro de una tarjeta. */
    val icon = RoundedCornerShape(12.dp)

    /** Píldoras / chips. */
    val pill = RoundedCornerShape(50)
}
