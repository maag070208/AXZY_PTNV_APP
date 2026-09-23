package com.axzydev.puertonuevoapp.core.util

import kotlin.random.Random

/**
 * Identificador único generado en el dispositivo. Es la garantía de
 * idempotencia de `POST /access/events` (`clientEventId`): el mismo escaneo
 * debe reutilizar el mismo id al reintentar, para que el backend no cree un
 * evento duplicado. Formato UUID v4 (sin dependencias externas).
 */
fun newClientEventId(): String {
    val bytes = Random.nextBytes(16)
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte() // versión 4
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte() // variante RFC 4122
    val hex = bytes.joinToString("") { byte ->
        val v = byte.toInt() and 0xff
        val s = v.toString(16)
        if (s.length == 1) "0$s" else s
    }
    return buildString(36) {
        append(hex.substring(0, 8)); append('-')
        append(hex.substring(8, 12)); append('-')
        append(hex.substring(12, 16)); append('-')
        append(hex.substring(16, 20)); append('-')
        append(hex.substring(20, 32))
    }
}
