package com.axzydev.puertonuevoapp.feature.catalogs

import com.axzydev.puertonuevoapp.core.network.hr.DocumentTypeCreateInput
import com.axzydev.puertonuevoapp.core.network.hr.DocumentTypeUpdateInput
import com.axzydev.puertonuevoapp.core.network.hr.HrApi
import com.axzydev.puertonuevoapp.core.network.hr.HrCatalogUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketCategoryUpdateInput
import com.axzydev.puertonuevoapp.core.network.tickets.TicketsApi
import com.axzydev.puertonuevoapp.core.session.PermissionKeys

/**
 * Fuentes de los catálogos simples de la pantalla. Cada una envuelve las
 * llamadas reales del API (los catálogos de RH viven en [HrApi] y las
 * categorías de ticket en [TicketsApi]) para que la pestaña y su ViewModel
 * sean genéricos.
 *
 * Permisos (verificados en las rutas del API):
 * - Lectura de `hr/catalogs` (document-types, genders, blood-types): `hr.records`; alta/edición/baja: `catalogs.manage`.
 * - Categorías de ticket: lectura abierta a la sesión; CUD `catalogs.manage`.
 */
fun simpleCatalogSources(hrApi: HrApi, ticketsApi: TicketsApi): Map<CatalogsTab, SimpleCatalogSource> {
    val hrDenied = "Se requiere el permiso de expedientes (hr.records) para consultar este catálogo."

    val documentTypes = SimpleCatalogSource(
        label = "Tipos de documento",
        singular = "tipo de documento",
        emptyMessage = "Aún no hay tipos de documento.",
        viewPermission = PermissionKeys.HR_RECORDS,
        deniedMessage = hrDenied,
        list = { includeInactive ->
            hrApi.documentTypes(includeInactive).map { SimpleCatalogEntry(it.id, it.name, it.active) }
        },
        create = { name -> hrApi.createDocumentType(DocumentTypeCreateInput(name = name)) },
        update = { id, name, active ->
            hrApi.updateDocumentType(id, DocumentTypeUpdateInput(name = name, active = active))
        },
        remove = { id -> hrApi.removeDocumentType(id) },
    )

    val genders = SimpleCatalogSource(
        label = "Géneros",
        singular = "género",
        emptyMessage = "Aún no hay géneros.",
        viewPermission = PermissionKeys.HR_RECORDS,
        deniedMessage = hrDenied,
        list = { includeInactive ->
            hrApi.genders(includeInactive).map { SimpleCatalogEntry(it.id, it.name, it.active) }
        },
        create = { name -> hrApi.createGender(name) },
        update = { id, name, active ->
            hrApi.updateGender(id, HrCatalogUpdateInput(name = name, active = active))
        },
        remove = { id -> hrApi.removeGender(id) },
    )

    val bloodTypes = SimpleCatalogSource(
        label = "Tipos de sangre",
        singular = "tipo de sangre",
        emptyMessage = "Aún no hay tipos de sangre.",
        viewPermission = PermissionKeys.HR_RECORDS,
        deniedMessage = hrDenied,
        list = { includeInactive ->
            hrApi.bloodTypes(includeInactive).map { SimpleCatalogEntry(it.id, it.name, it.active) }
        },
        create = { name -> hrApi.createBloodType(name) },
        update = { id, name, active ->
            hrApi.updateBloodType(id, HrCatalogUpdateInput(name = name, active = active))
        },
        remove = { id -> hrApi.removeBloodType(id) },
    )

    val ticketCategories = SimpleCatalogSource(
        label = "Categorías de ticket",
        singular = "categoría",
        emptyMessage = "Aún no hay categorías de ticket.",
        viewPermission = null,
        list = { includeInactive ->
            ticketsApi.categories(includeInactive).map { SimpleCatalogEntry(it.id, it.name, it.active) }
        },
        create = { name -> ticketsApi.createCategory(name) },
        update = { id, name, active ->
            ticketsApi.updateCategory(id, TicketCategoryUpdateInput(name = name, active = active))
        },
        remove = { id -> ticketsApi.removeCategory(id) },
    )

    return mapOf(
        CatalogsTab.DocumentTypes to documentTypes,
        CatalogsTab.Genders to genders,
        CatalogsTab.BloodTypes to bloodTypes,
        CatalogsTab.TicketCategories to ticketCategories,
    )
}
