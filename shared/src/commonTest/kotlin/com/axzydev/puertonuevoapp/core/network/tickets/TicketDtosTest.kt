package com.axzydev.puertonuevoapp.core.network.tickets

import com.axzydev.puertonuevoapp.core.network.http.apiJson
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Contrato de tickets con la API: la categoría es un catálogo (objeto o null). */
class TicketDtosTest {

    private val base = """
        "id":"t1","titulo":"Fuga","descripcion":"Baño 2","status":"ABIERTO","priority":"ALTA",
        "creadoPorId":"u1","creadoPor":{"id":"u1","name":"Ana Palma","username":"apalma"},
        "creadoEn":"2026-09-24T10:00:00.000Z","actualizadoEn":"2026-09-24T10:00:00.000Z"
    """.trimIndent()

    @Test
    fun decodesCategoryObject() {
        val ticket = apiJson.decodeFromString<TicketDto>(
            """{$base,"categoryId":"c1","category":{"id":"c1","nombre":"Plomería","activo":true}}""",
        )
        assertEquals("c1", ticket.categoryId)
        assertEquals("Plomería", ticket.category?.nombre)
    }

    @Test
    fun decodesTicketWithoutCategory() {
        val ticket = apiJson.decodeFromString<TicketDto>("""{$base,"categoryId":null,"category":null}""")
        assertNull(ticket.categoryId)
        assertNull(ticket.category)
    }

    @Test
    fun updateSendsCategoryOnlyWhenItChanged() {
        val untouched = apiJson.encodeToString(TicketUpdateInput.serializer(), TicketUpdateInput(titulo = "Fuga"))
        assertFalse("categoryId" in untouched)

        val cleared = apiJson.encodeToString(TicketUpdateInput.serializer(), TicketUpdateInput(categoryId = JsonNull))
        assertTrue("\"categoryId\":null" in cleared, cleared)

        val changed = apiJson.encodeToString(TicketUpdateInput.serializer(), TicketUpdateInput(categoryId = JsonPrimitive("c2")))
        assertTrue("\"categoryId\":\"c2\"" in changed, changed)
    }
}
