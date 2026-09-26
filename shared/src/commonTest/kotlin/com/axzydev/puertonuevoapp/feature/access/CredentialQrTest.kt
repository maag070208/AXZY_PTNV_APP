package com.axzydev.puertonuevoapp.feature.access

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialQrTest {

    @Test
    fun validFullPayload() {
        val raw = """{"v":2,"id":"emp-1","no":"E-001","name":"Ana Palma","pos":"Analista","dept":"Sistemas"}"""
        val result = parseCredentialPayload(raw)
        val valid = assertIs<QrParseResult.Valid>(result)
        assertEquals(2, valid.payload.version)
        assertEquals("emp-1", valid.payload.employeeId)
        assertEquals("E-001", valid.payload.employeeNumber)
        assertEquals("Ana Palma", valid.payload.name)
        assertEquals("Analista", valid.payload.jobTitle)
        assertEquals("Sistemas", valid.payload.department)
    }

    @Test
    fun validMinimalPayloadOmitsOptionalFields() {
        val result = parseCredentialPayload("""{"v":2,"id":"emp-2"}""")
        val valid = assertIs<QrParseResult.Valid>(result)
        assertEquals("emp-2", valid.payload.employeeId)
        assertNull(valid.payload.employeeNumber)
        assertNull(valid.payload.name)
        assertNull(valid.payload.jobTitle)
        assertNull(valid.payload.department)
    }

    @Test
    fun versionAsStringIsAccepted() {
        val result = parseCredentialPayload("""{"v":"2","id":"emp-3"}""")
        assertIs<QrParseResult.Valid>(result)
    }

    @Test
    fun plainTextIsInvalid() {
        val result = parseCredentialPayload("esto no es un QR")
        assertIs<QrParseResult.Invalid>(result)
    }

    @Test
    fun brokenJsonIsInvalid() {
        val result = parseCredentialPayload("{v:2,id:}")
        assertIs<QrParseResult.Invalid>(result)
    }

    @Test
    fun missingIdIsInvalid() {
        val result = parseCredentialPayload("""{"v":2}""")
        val invalid = assertIs<QrParseResult.Invalid>(result)
        assertTrue(invalid.reason.contains("identificador", ignoreCase = true))
    }

    @Test
    fun unsupportedVersionIsInvalid() {
        val result = parseCredentialPayload("""{"v":3,"id":"emp-4"}""")
        val invalid = assertIs<QrParseResult.Invalid>(result)
        assertTrue(invalid.reason.contains("v:3"))
    }

    @Test
    fun nonObjectJsonIsInvalid() {
        val result = parseCredentialPayload("[1,2,3]")
        assertIs<QrParseResult.Invalid>(result)
    }

    @Test
    fun blankInputIsInvalid() {
        val result = parseCredentialPayload("   ")
        assertIs<QrParseResult.Invalid>(result)
    }

    @Test
    fun ownCredentialQrIsReadByTheScanner() {
        val qr = buildCredentialQr("emp-9")
        assertEquals("""{"v":2,"id":"emp-9"}""", qr)
        val valid = assertIs<QrParseResult.Valid>(parseCredentialPayload(qr))
        assertEquals(CREDENTIAL_VERSION, valid.payload.version)
        assertEquals("emp-9", valid.payload.employeeId)
    }
}
