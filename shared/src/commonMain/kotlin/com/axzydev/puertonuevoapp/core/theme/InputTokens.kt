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
    /** Radio de esquina de todos los inputs. */
    val Shape = RoundedCornerShape(10.dp)

    /** Altura mínima táctil del campo (multilínea). */
    val MinHeight = 44.dp

    /** Altura exacta de los campos de una sola línea. */
    val Height = 44.dp

    /** Padding horizontal interno del contenido. */
    val ContentPaddingHorizontal = 14.dp

    /** Padding vertical interno del contenido. */
    val ContentPaddingVertical = 10.dp
}
