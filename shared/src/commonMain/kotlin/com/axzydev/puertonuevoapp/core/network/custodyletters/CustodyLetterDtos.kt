package com.axzydev.puertonuevoapp.core.network.custodyletters

import com.axzydev.puertonuevoapp.core.network.common.DepartmentRefDto
import com.axzydev.puertonuevoapp.core.network.common.UserRefDto
import com.axzydev.puertonuevoapp.core.network.devices.DeviceDto
import com.axzydev.puertonuevoapp.core.network.locations.LocationDto
import kotlinx.serialization.Serializable

@Serializable
data class SequencePeekDto(val next: String)

@Serializable
data class SequenceStateDto(
    val prefix: String,
    val counter: Int,
    val next: String,
)

@Serializable
data class CustodyLetterPersonRefDto(
    val id: String,
    val name: String,
    val jobTitle: String? = null,
    val employeeNumber: String? = null,
    val department: DepartmentRefDto? = null,
)

@Serializable
data class CustodyLetterItemDto(
    val id: String? = null,
    val custodyLetterId: String? = null,
    val deviceId: String? = null,
    val device: DeviceDto? = null,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val assetTag: String? = null,
    val area: String? = null,
)

@Serializable
data class CustodyLetterDto(
    val id: String,
    val consecutive: String,
    val date: String,
    val employeeNumber: String,
    val company: String? = null,
    val department: String,
    val areaBoss: String? = null,
    val deliveryBy: String? = null,
    val createdById: String? = null,
    val createdBy: UserRefDto? = null,
    val custodianId: String? = null,
    val custodian: CustodyLetterPersonRefDto? = null,
    val supervisorId: String? = null,
    val supervisor: CustodyLetterPersonRefDto? = null,
    val locationId: String? = null,
    val location: LocationDto? = null,
    val items: List<CustodyLetterItemDto> = emptyList(),
    val returnDate: String? = null,
    val returnedBy: String? = null,
    val returnCondition: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
)

@Serializable
data class CustodyLettersListResponseDto(
    val data: List<CustodyLetterDto>,
    val total: Int = 0,
)

@Serializable
data class CustodyLetterItemInput(
    val deviceId: String? = null,
    val description: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val hostname: String? = null,
    val assetTag: String? = null,
    val area: String? = null,
)

@Serializable
data class CustodyLetterCreateInput(
    val consecutive: String? = null,
    val date: String? = null,
    val employeeNumber: String,
    val company: String? = null,
    val department: String? = null,
    val areaBoss: String? = null,
    val deliveryBy: String? = null,
    val custodianId: String? = null,
    val supervisorId: String? = null,
    val item: CustodyLetterItemInput,
)

/** Update parcial: `item` nulo = "no tocar el item actual". */
@Serializable
data class CustodyLetterUpdateInput(
    val date: String? = null,
    val employeeNumber: String? = null,
    val company: String? = null,
    val department: String? = null,
    val areaBoss: String? = null,
    val deliveryBy: String? = null,
    val custodianId: String? = null,
    val supervisorId: String? = null,
    val item: CustodyLetterItemInput? = null,
)

@Serializable
data class CustodyLetterReturnInput(
    val returnedBy: String,
    val returnCondition: String,
)

@Serializable
data class CustodyLetterGenerateInput(val typeId: String)

@Serializable
data class CustodyLetterGeneratedTypeDto(
    val code: String,
    val name: String,
    val prefix: String,
)

@Serializable
data class CustodyLetterGeneratedDto(
    val type: CustodyLetterGeneratedTypeDto,
    val counter: Int,
    val custodyLetter: CustodyLetterDto,
)
