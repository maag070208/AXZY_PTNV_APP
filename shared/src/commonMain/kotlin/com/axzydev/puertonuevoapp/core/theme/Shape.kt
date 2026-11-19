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

    /** Segmento de barra segmentada bento (4dp). */
    val segment = RoundedCornerShape(4.dp)

    /** Tarjetas y mosaicos del diseño bento (24dp). */
    val bento = RoundedCornerShape(24.dp)

    /** Hoja inferior que se superpone a un encabezado (esquinas superiores 28dp). */
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Panel lateral (drawer) que se desliza desde la izquierda: esquinas derechas 28dp. */
    val sheetEnd = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)

    // --- Tokens del chrome "C" (header, barra inferior, filas bento) ---

    /** Botón cuadrado redondeado del header (44dp, radio 16). */
    val chromeButton = RoundedCornerShape(16.dp)

    /** Barra inferior flotante (radio 24). */
    val chromeBar = RoundedCornerShape(24.dp)

    /** Tarjetas / listas del diseño C (radio 20). */
    val listCard = RoundedCornerShape(20.dp)

    /** Cuadro de ícono de acceso rápido (52dp, radio 16). */
    val iconTile = RoundedCornerShape(16.dp)

    /** Cuadro de ícono de fila de lista grande (46dp, radio 15). */
    val iconRowLarge = RoundedCornerShape(15.dp)

    /** Cuadro de ícono de fila compacta (42dp, radio 14) y botones de acción de fila. */
    val iconRowSmall = RoundedCornerShape(14.dp)
}
