package com.axzydev.puertonuevoapp.core.network.custodyletters

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.http.ApiException
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

/**
 * Adaptador al contrato REAL del API. Las "cartas responsivas" del contrato
 * documentado (`/cartas`) son **préstamos** en el backend actual
 * (`/inventory/loans`). La app conserva su modelo [CustodyLetterDto] y aquí se
 * traduce.
 *
 * Degradaciones conocidas (no existen en el API real): folios anticipados
 * (`sequence/peek`), `reset` y `generate` por tipo. `undoReturn` no tiene
 * endpoint; la devolución se registra vía `/inventory/returns`.
 */
class CustodyLettersApi(private val client: ApiClient) {
    suspend fun list(q: String? = null): CustodyLettersListResponseDto {
        val all = client.get<List<ApiLoan>>("/inventory/loans").map { it.toCustodyLetterDto() }
        val filtered = q?.takeIf { it.isNotBlank() }
            ?.let { query -> all.filter { it.consecutive.contains(query, true) || it.department.contains(query, true) } }
            ?: all
        return CustodyLettersListResponseDto(data = filtered, total = filtered.size)
    }

    suspend fun get(id: String): CustodyLetterDto =
        client.get<ApiLoan>("/inventory/loans/$id").toCustodyLetterDto()

    suspend fun create(input: CustodyLetterCreateInput): CustodyLetterDto {
        val body = ApiCreateLoan(
            custodianId = input.custodianId,
            notes = input.deliveryBy,
            items = input.item.deviceId?.let { listOf(ApiLoanItemInput(it, 1)) } ?: emptyList(),
        )
        return client.post<ApiCreateLoan, ApiLoan>("/inventory/loans", body).toCustodyLetterDto()
    }

    suspend fun update(id: String, input: CustodyLetterUpdateInput): CustodyLetterDto {
        val body = ApiUpdateLoan(
            custodianId = input.custodianId,
            deviceId = input.item?.deviceId,
            quantity = input.item?.deviceId?.let { 1 },
        )
        return client.put<ApiUpdateLoan, ApiLoan>("/inventory/loans/$id", body).toCustodyLetterDto()
    }

    suspend fun remove(id: String) = client.postNoContent("/inventory/loans/$id/cancel")

    /** No existe folio global en el API real. */
    suspend fun sequence(): SequenceStateDto = SequenceStateDto(prefix = "", counter = 0, next = "")

    /** No existe folio anticipado en el API real. */
    suspend fun peekSequence(): String = ""

    /** No existe reset de consecutivo en el API real. */
    suspend fun resetSequence(): SequenceStateDto =
        throw ApiException(501, "Reiniciar consecutivo no está disponible en el API actual")

    /** No existe generación de plantilla por tipo en el API real. */
    suspend fun generate(typeId: String): CustodyLetterGeneratedDto =
        throw ApiException(501, "Generar carta por tipo no está disponible en el API actual")

    suspend fun returnCustodyLetter(id: String, returnedBy: String, returnCondition: String): CustodyLetterDto {
        val loan = client.get<ApiLoan>("/inventory/loans/$id")
        // `condition` es un enum del API (GOOD/FAIR/POOR/BROKEN); las
        // condiciones que captura la app son texto libre y viajan en `notes`.
        val pending = loan.items.mapNotNull { d ->
            val remaining = d.quantity - d.returnedQuantity
            if (remaining > 0) ApiLoanReturnItemInput(d.id, remaining, "GOOD") else null
        }
        if (pending.isNotEmpty()) {
            client.post<ApiCreateLoanReturn, ApiLoanReturn>(
                "/inventory/returns",
                ApiCreateLoanReturn(loanId = id, notes = "$returnedBy — $returnCondition", items = pending),
            )
        }
        return client.get<ApiLoan>("/inventory/loans/$id").toCustodyLetterDto()
    }

    /** No existe endpoint para deshacer la devolución en el API real. */
    suspend fun undoReturn(id: String): CustodyLetterDto =
        throw ApiException(501, "Deshacer devolución no está disponible en el API actual")
}

private fun ApiLoan.toCustodyLetterDto(): CustodyLetterDto = CustodyLetterDto(
    id = id,
    consecutive = number,
    date = date,
    employeeNumber = custodian?.employeeNumber ?: "",
    department = department?.name ?: custodian?.department?.name ?: "",
    custodianId = custodianId,
    custodian = custodian?.let {
        CustodyLetterPersonRefDto(
            id = it.id,
            name = it.name,
            employeeNumber = it.employeeNumber,
            department = it.department?.let { d -> DepartmentRefDto(id = d.id, name = d.name) },
        )
    },
    items = items.map { d ->
        CustodyLetterItemDto(
            id = d.id,
            custodyLetterId = id,
            deviceId = d.deviceId,
            description = d.device?.name,
            brand = d.device?.brand,
            model = d.device?.model,
        )
    },
    returnDate = returns.firstOrNull()?.date,
    createdAt = date,
)

@Serializable
private data class ApiDeptRef(val id: String, val name: String)

@Serializable
private data class ApiCustodian(
    val id: String,
    val name: String,
    val username: String = "",
    val employeeNumber: String? = null,
    val department: ApiDeptRef? = null,
)

@Serializable
private data class ApiLoanDevice(
    val id: String,
    val name: String = "",
    val brand: String = "",
    val model: String = "",
)

@Serializable
private data class ApiLoanItem(
    val id: String,
    val deviceId: String = "",
    val device: ApiLoanDevice? = null,
    val quantity: Int = 0,
    val returnedQuantity: Int = 0,
)

@Serializable
private data class ApiLoanReturnRef(val id: String, val number: String = "", val date: String = "")

@Serializable
private data class ApiLoan(
    val id: String,
    val custodianId: String? = null,
    val custodian: ApiCustodian? = null,
    val departmentId: String? = null,
    val department: ApiDeptRef? = null,
    val subareaId: String? = null,
    val date: String = "",
    val status: String = "ACTIVE",
    val number: String = "",
    val notes: String? = null,
    val items: List<ApiLoanItem> = emptyList(),
    val returns: List<ApiLoanReturnRef> = emptyList(),
)

@Serializable
private data class ApiLoanItemInput(val deviceId: String, val quantity: Int = 1)

@Serializable
private data class ApiCreateLoan(
    val custodianId: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
    val notes: String? = null,
    val items: List<ApiLoanItemInput> = emptyList(),
)

@Serializable
private data class ApiUpdateLoan(
    val custodianId: String? = null,
    val departmentId: String? = null,
    val subareaId: String? = null,
    val notes: String? = null,
    val deviceId: String? = null,
    val quantity: Int? = null,
)

@Serializable
private data class ApiLoanReturnItemInput(
    val loanItemId: String,
    val quantity: Int,
    val condition: String,
)

@Serializable
private data class ApiCreateLoanReturn(
    val loanId: String,
    val custodianId: String? = null,
    val notes: String? = null,
    val items: List<ApiLoanReturnItemInput> = emptyList(),
)

@Serializable
private data class ApiLoanReturn(val id: String = "", val loanId: String = "")
