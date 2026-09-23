package com.axzydev.puertonuevoapp.feature.access

import com.axzydev.puertonuevoapp.core.network.access.AccessEventType
import com.axzydev.puertonuevoapp.core.network.access.AccessLastEventDto

/**
 * Deriva el tipo de evento sugerido a partir del último evento del empleado:
 * si la última marca fue una ENTRADA, toca registrar la SALIDA (y viceversa).
 * Espejo de la regla del backend (`suggestedType`).
 */
fun suggestedTypeFromLastEvent(lastEvent: AccessLastEventDto?): String =
    if (lastEvent?.type == AccessEventType.ENTRY) AccessEventType.EXIT else AccessEventType.ENTRY
