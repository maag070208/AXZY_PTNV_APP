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

    /** Tarjetas y mosaicos del diseño bento (24dp). */
    val bento = RoundedCornerShape(24.dp)

    /** Hoja inferior que se superpone a un encabezado (esquinas superiores 28dp). */
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Panel lateral (drawer) que se desliza desde la izquierda: esquinas derechas 28dp. */
    val sheetEnd = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
}
