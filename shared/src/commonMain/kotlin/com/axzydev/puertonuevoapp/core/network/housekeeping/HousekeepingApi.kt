package com.axzydev.puertonuevoapp.core.network.housekeeping

import com.axzydev.puertonuevoapp.core.network.http.ApiClient
import com.axzydev.puertonuevoapp.core.network.linen.EmptyBody
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

/** Habitación de una tarea de limpieza. */
@Serializable
data class HousekeepingRoomDto(
    val id: String,
    val number: String = "",
    val status: String = "",
)

@Serializable
data class HousekeepingInspectionRefDto(val id: String = "", val result: String? = null)

/** Tarea de limpieza (`GET /hotel/tasks`); estados PENDING / IN_PROGRESS / FINISHED / CANCELLED. */
@Serializable
data class HousekeepingTaskDto(
    val id: String,
    val roomId: String = "",
    val cleaningType: String = "CHECKOUT",
    val status: String,
    val assignedToId: String? = null,
    val requiresInspection: Boolean = false,
    val isRework: Boolean = false,
    val kitAlert: Boolean = false,
    val date: String = "",
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val room: HousekeepingRoomDto? = null,
    val inspection: HousekeepingInspectionRefDto? = null,
)

/** Motivo del catálogo (`GET /hotel/reasons?kind=INSPECTION`). */
@Serializable
data class HousekeepingReasonDto(
    val id: String,
    val name: String = "",
    val requiresNotes: Boolean = false,
    val active: Boolean = true,
)

/** Cuerpo de `POST /hotel/tasks/:id/inspect`. */
@Serializable
data class InspectInput(
    val result: String,
    val reasonId: String? = null,
    val notes: String? = null,
)

interface HousekeepingApi {
    /** Tareas visibles al usuario (la camarista solo ve las suyas). */
    suspend fun tasks(pendingInspection: Boolean = false): List<HousekeepingTaskDto>

    /** Detalle (incluye `inspection` y el estado actual de la habitación). */
    suspend fun task(id: String): HousekeepingTaskDto

    suspend fun start(id: String)

    suspend fun finish(id: String)

    suspend fun inspect(id: String, input: InspectInput)

    suspend fun inspectionReasons(): List<HousekeepingReasonDto>
}

class HousekeepingApiClient(private val client: ApiClient) : HousekeepingApi {
    override suspend fun tasks(pendingInspection: Boolean): List<HousekeepingTaskDto> =
        client.get(if (pendingInspection) "/hotel/tasks?pendingInspection=true" else "/hotel/tasks")

    override suspend fun task(id: String): HousekeepingTaskDto = client.get("/hotel/tasks/${id.encodeURLParameter()}")

    override suspend fun start(id: String) =
        client.postNoContent("/hotel/tasks/${id.encodeURLParameter()}/start", EmptyBody())

    override suspend fun finish(id: String) =
        client.postNoContent("/hotel/tasks/${id.encodeURLParameter()}/finish", EmptyBody())

    override suspend fun inspect(id: String, input: InspectInput) =
        client.postNoContent("/hotel/tasks/${id.encodeURLParameter()}/inspect", input)

    override suspend fun inspectionReasons(): List<HousekeepingReasonDto> =
        client.get("/hotel/reasons?kind=INSPECTION")
}
