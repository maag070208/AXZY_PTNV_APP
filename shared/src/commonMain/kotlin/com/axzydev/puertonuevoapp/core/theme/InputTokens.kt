package com.axzydev.puertonuevoapp.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Fuente ÚNICA de verdad para la apariencia de los campos de texto.
 *
 * Ninguna pantalla debe fijar `shape`, colores, altura ni paddings de un
 * input: todo sale de aquí a través de `AppTextField`/`AppPasswordField`
 * (`core/ui/AppTextField.kt`). Cambiar un valor aquí cambia toda la app.
 */
object InputTokens {
    /** Forma de los campos de una sola línea (bento: píldora). */
    val Shape = RoundedCornerShape(50)

    /** Forma de los campos multilínea (una píldora deformaría el texto). */
    val MultilineShape = RoundedCornerShape(20.dp)

    /** Grosor del aro de foco / error. */
    val RingWidth = 2.dp

    /** Altura mínima táctil del campo (multilínea). */
    val MinHeight = 48.dp

    /** Altura exacta de los campos de una sola línea. */
    val Height = 48.dp

    /** Padding horizontal interno del contenido. */
    val ContentPaddingHorizontal = 18.dp

    /** Padding vertical interno del contenido. */
    val ContentPaddingVertical = 10.dp
}
